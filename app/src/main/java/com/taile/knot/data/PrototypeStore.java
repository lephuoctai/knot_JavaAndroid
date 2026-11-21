package com.taile.knot.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Base64;
import com.taile.knot.BuildConfig;
import com.taile.knot.data.model.Knot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Local API client. Email is an unverified label; the server issues a device session. */
public final class PrototypeStore {
    public static final class ApiFailure extends IllegalStateException {
        public final int status;
        ApiFailure(int status,String message){super(message);this.status=status;}
    }
    private final SharedPreferences prefs;
    private final Context context;
    public PrototypeStore(Context context) {
        this.context=context.getApplicationContext();
        prefs=this.context.getSharedPreferences("knot_client_session",Context.MODE_PRIVATE);
    }
    public boolean hasEmail(){return !prefs.getString("email","").isEmpty()&&!token().isEmpty();}
    public String journey(){return prefs.getString("journeyCode","");}
    public String journeyId(){return prefs.getString("journeyId","");}
    private String token(){return prefs.getString("token","");}
    public boolean setEmail(String email) {
        try {
            JSONObject response=request("POST","/v1/sessions",new JSONObject().put("email",email),"");
            synchronized(CACHE_LOCK){clearOffline();return prefs.edit().putString("email",response.getString("email"))
                    .putString("token",response.getString("token"))
                    .remove("journeyCode").remove("journeyId").commit();}
        }catch(JSONException error){throw new IllegalStateException("Phản hồi server không hợp lệ.");}
    }
    public boolean createJourney(){return select(request("POST","/v1/journeys",new JSONObject(),token()));}
    public boolean joinJourney(String code){
        try{return select(request("POST","/v1/journeys/join",new JSONObject().put("code",code),token()));}
        catch(JSONException error){throw new IllegalStateException("Mã không hợp lệ.");}
    }
    private boolean select(JSONObject response){
        try{synchronized(CACHE_LOCK){clearOffline();return prefs.edit().putString("journeyId",response.getString("id"))
                .putString("journeyCode",response.getString("code")).commit();}}
        catch(JSONException error){throw new IllegalStateException("Phản hồi server không hợp lệ.");}
    }
    private static final Object CACHE_LOCK=new Object();
    private static long refreshGeneration;
    private String identity(){return BuildConfig.KNOT_API_URL+"|"+prefs.getString("email","")+"|"+journeyId()+"|"+token();}
    public String syncNotice="";
    public boolean needsRefresh(){return prefs.getBoolean("timelineDirty",false);}
    public List<Knot> cachedKnots() throws JSONException {
        synchronized(CACHE_LOCK){JSONObject value=new OfflineTimeline(context,identity()).read();
            return value==null?null:decode(value,new OfflineTimeline(context,identity()));}
    }
    public List<Knot> knots() throws JSONException {
        final String selected=identity(),jid=journeyId(),bearer=token();
        OfflineTimeline cache=new OfflineTimeline(context,selected);JSONObject previous;final long generation;
        synchronized(CACHE_LOCK){generation=++refreshGeneration;previous=cache.read();}
        boolean complete=previous!=null;
        if(previous!=null){JSONArray items=previous.getJSONArray("knots");for(int i=0;i<items.length();i++)
            if(!cache.image(items.getJSONObject(i).getString("id")).isFile())complete=false;}
        String etag=complete?previous.optString("_etag",""):"";
        JSONObject current=request("GET","/v1/journeys/"+jid,null,bearer,etag);
        if(current.optBoolean("_notModified"))current=previous;
        if(current==null)throw new IllegalStateException("Chưa có dữ liệu local. Refresh lại nha.");
        JSONArray items=current.getJSONArray("knots");java.util.Set<String> ids=new java.util.HashSet<>();
        for(int i=0;i<items.length();i++)ids.add(items.getJSONObject(i).getString("id"));
        synchronized(CACHE_LOCK){
            if(!selected.equals(identity())||generation!=refreshGeneration)throw new IllegalStateException("Hành trình đã thay đổi.");
            try{cache.write(current);cache.prune(ids);}catch(IOException e){throw new IllegalStateException("Không đủ bộ nhớ để lưu offline.");}
        }
        int missing=0;
        for(int i=0;i<items.length();i++) {
            JSONObject item=items.getJSONObject(i);String id=item.getString("id");if(cache.image(id).isFile())continue;
            HttpURLConnection connection=null;
            try {
                URL url=new URL(item.getString("imageUrl"));URL api=new URL(BuildConfig.KNOT_API_URL);
                if(!url.getProtocol().equals(api.getProtocol())||!url.getHost().equals(api.getHost())||url.getPort()!=api.getPort())throw new IOException();
                connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);
                byte[] bytes;try(InputStream in=connection.getInputStream()){bytes=OfflineTimeline.readBounded(in,4*1024*1024);}
                if(bytes.length<2||bytes[0]!=(byte)0xff||bytes[1]!=(byte)0xd8)throw new IOException();
                synchronized(CACHE_LOCK){if(!selected.equals(identity())||generation!=refreshGeneration)throw new IllegalStateException("Hành trình đã thay đổi.");cache.saveImage(id,bytes);}
            }catch(IOException error){missing++;}finally{if(connection!=null)connection.disconnect();}
        }
        synchronized(CACHE_LOCK){
            if(!selected.equals(identity())||generation!=refreshGeneration)throw new IllegalStateException("Hành trình đã thay đổi.");
            prefs.edit().remove("timelineDirty").apply();
            syncNotice=missing>0?"Đã cập nhật; còn "+missing+" ảnh chưa tải để xem offline.":"Đã cập nhật. Có thể xem offline.";
            return decode(current,cache);
        }
    }
    private List<Knot> decode(JSONObject response,OfflineTimeline cache) throws JSONException {
        JSONArray array=response.getJSONArray("knots");List<Knot> knots=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            JSONObject item=array.getJSONObject(i);String id=item.getString("id");java.io.File file=cache.image(id);
            String image=file.isFile()?Uri.fromFile(file).toString():item.optString("imageUrl","");
            Knot knot=new Knot(id,item.getString("description"),image,item.getLong("memoryDate"));
            knot.setThumbnailUrl(file.isFile()?image:item.optString("thumbnailUrl",""));knots.add(knot);
        }
        return knots;
    }
    public boolean addKnot(Knot knot) throws JSONException {
        final String id=journeyId(),bearer=token();
        if(id.isEmpty()||bearer.isEmpty())throw new IllegalStateException("Bạn cần chọn hành trình trước.");
        try {
            Bitmap image=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),Uri.parse(knot.getImageUrl())),
                    (decoder,info,source)->{
                        int width=info.getSize().getWidth(),height=info.getSize().getHeight();
                        float scale=Math.min(1f,1600f/Math.max(width,height));
                        decoder.setTargetSize(Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale)));
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
            float scale=Math.min(1f,360f/Math.max(image.getWidth(),image.getHeight()));
            Bitmap thumb=Bitmap.createScaledBitmap(image,Math.max(1,Math.round(image.getWidth()*scale)),
                    Math.max(1,Math.round(image.getHeight()*scale)),true);
            byte[] full=jpeg(image,3*1024*1024),small=jpeg(thumb,512*1024);
            if(thumb!=image)thumb.recycle();image.recycle();
            String fingerprint=id+":"+knot.getImageUrl()+":"+knot.getMemoryDate()+":"+knot.getDescription();
            String operationId;
            synchronized(PrototypeStore.class){
                if(fingerprint.equals(prefs.getString("pendingFingerprint","")))operationId=prefs.getString("pendingId",knot.getId());
                else {operationId=UUID.randomUUID().toString();
                    if(!prefs.edit().putString("pendingFingerprint",fingerprint).putString("pendingId",operationId).commit())
                        throw new IllegalStateException("Chưa chuẩn bị lưu được. Thử lại nha.");}
            }
            JSONObject payload=new JSONObject().put("id",operationId).put("description",knot.getDescription())
                    .put("memoryDate",knot.getMemoryDate()).put("imageBase64",Base64.encodeToString(full,Base64.NO_WRAP))
                    .put("thumbnailBase64",Base64.encodeToString(small,Base64.NO_WRAP));
            request("POST","/v1/journeys/"+id+"/knots",payload,bearer);
            if(id.equals(journeyId())&&bearer.equals(token()))prefs.edit().putBoolean("timelineDirty",true).commit();
            synchronized(PrototypeStore.class){
                if(operationId.equals(prefs.getString("pendingId","")))
                    prefs.edit().remove("pendingFingerprint").remove("pendingId").commit();
            }
            return true;
        }catch(IOException error){throw new IllegalStateException("Chưa đọc được ảnh. Chọn lại ảnh nha.");}
    }
    private byte[] jpeg(Bitmap bitmap,int limit){
        for(int quality=85;quality>=45;quality-=10){
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            if(bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out)&&out.size()<=limit)return out.toByteArray();
        }
        throw new IllegalStateException("Ảnh quá lớn. Chọn ảnh khác nha.");
    }
    private JSONObject request(String method,String path,JSONObject body,String bearer){return request(method,path,body,bearer,"");}
    private JSONObject request(String method,String path,JSONObject body,String bearer,String etag){
        HttpURLConnection connection=null;
        try {
            URL endpoint=new URL(BuildConfig.KNOT_API_URL.replaceAll("/+$","")+path);
            if(!"https".equals(endpoint.getProtocol())&&!(BuildConfig.DEBUG&&"http".equals(endpoint.getProtocol())
                    &&("127.0.0.1".equals(endpoint.getHost())||"10.0.2.2".equals(endpoint.getHost())||"localhost".equals(endpoint.getHost()))))
                throw new IllegalStateException("Cấu hình server không hợp lệ.");
            connection=(HttpURLConnection)endpoint.openConnection();connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);
            if(!etag.isEmpty())connection.setRequestProperty("If-None-Match",etag);
            if(!bearer.isEmpty())connection.setRequestProperty("Authorization","Bearer "+bearer);
            if(body!=null){connection.setRequestProperty("Content-Type","application/json");connection.setDoOutput(true);
                try(OutputStream stream=connection.getOutputStream()){stream.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int status=connection.getResponseCode();
            if(status==304)return new JSONObject().put("_notModified",true);
            InputStream input=status>=200&&status<300?connection.getInputStream():connection.getErrorStream();
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            if(input!=null)try(InputStream stream=input){byte[] buffer=new byte[4096];int count;
                while((count=stream.read(buffer))!=-1){if(output.size()+count>4*1024*1024)throw new IOException();output.write(buffer,0,count);}}
            JSONObject response=new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            if(status<200||status>=300)throw new ApiFailure(status,response.optString("error","Chưa xử lý được. Thử lại nha."));
            String received=connection.getHeaderField("ETag");if(received!=null)response.put("_etag",received);
            return response;
        }catch(IOException error){throw new IllegalStateException("Chưa cập nhật được. Kiểm tra kết nối mạng; bản local vẫn được giữ.");}
        catch(JSONException error){throw new IllegalStateException("Phản hồi server không hợp lệ.");}
        finally{if(connection!=null)connection.disconnect();}
    }
    // Passive local reset is separate from a confirmed server membership change.
    public void leaveJourney(){synchronized(CACHE_LOCK){clearOffline();prefs.edit().remove("journeyId").remove("journeyCode").remove("pendingId").remove("pendingFingerprint").commit();}}
    public void leaveScreen(){synchronized(CACHE_LOCK){clearOffline();prefs.edit().clear().commit();}}
    private void clearOffline(){refreshGeneration++;OfflineTimeline.clear(context);}
    public JSONObject prepareDeparture(boolean logout){
        try{return request("POST","/v1/journeys/"+journeyId()+"/leave-preview",
                new JSONObject().put("action",logout?"logout":"switch"),token());}
        catch(JSONException error){throw new IllegalStateException("Chưa chuẩn bị được xác nhận rời.");}
    }
    public JSONObject depart(boolean logout,JSONObject preview){
        try {
            JSONObject result=request("POST","/v1/journeys/"+journeyId()+"/leave",
                    new JSONObject().put("action",logout?"logout":"switch").put("ticket",preview.getString("ticket")),token());
            if(logout){
                try{request("POST","/v1/logout",new JSONObject(),token());}
                // Membership removal is already acknowledged. A lost logout response must not strand the UI.
                catch(RuntimeException ignored){}
                leaveScreen();
            }
            else leaveJourney();
            return result;
        }catch(JSONException error){throw new IllegalStateException("Phản hồi rời hành trình không hợp lệ.");}
    }
    static String journeyCode(String email,LocalDate date){return email.substring(0,email.indexOf('@')).toLowerCase(Locale.ROOT)
            +date.format(DateTimeFormatter.ofPattern("ddMMuuuu",Locale.ROOT));}
}
