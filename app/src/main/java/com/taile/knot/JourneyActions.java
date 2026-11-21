package com.taile.knot;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.taile.knot.data.PrototypeStore;

final class JourneyActions {
    private static final int RELOAD = 1;
    private static final int SWITCH = 2;
    private static final int SIGN_OUT = 3;

    static void bind(Activity activity, TextView codeView, View settings,
                     PrototypeStore store, Runnable reload) {
        codeView.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(
                    activity.getString(R.string.journey_label), store.journey()));
            Toast.makeText(activity, "Đã sao chép mã", Toast.LENGTH_SHORT).show();
        });
        settings.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, settings);
            popup.getMenu().add(0, RELOAD, 0, R.string.reload_action);
            popup.getMenu().add(0, SWITCH, 1, R.string.switch_journey_action);
            popup.getMenu().add(0, SIGN_OUT, 2, R.string.sign_out_action);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == RELOAD) {
                    reload.run();
                    return true;
                }
                if (item.getItemId() != SWITCH && item.getItemId() != SIGN_OUT) return false;
                confirmDeparture(activity,settings,store,item.getItemId()==SIGN_OUT);
                return true;
            });
            popup.show();
        });
    }

    private static void confirmDeparture(Activity activity,View settings,PrototypeStore store,boolean logout){
        settings.setEnabled(false);
        new Thread(()->{
            try {
                org.json.JSONObject preview=store.prepareDeparture(logout);
                activity.runOnUiThread(()->{
                    settings.setEnabled(true);
                    if(activity.isFinishing()||activity.isDestroyed())return;
                    boolean last=preview.optInt("memberCount")==1;
                    new AlertDialog.Builder(activity).setTitle(R.string.leave_warning_title)
                        .setMessage(last?R.string.leave_warning_last:R.string.leave_warning_member)
                        .setNegativeButton(R.string.leave_cancel,null)
                        .setPositiveButton(R.string.leave_continue,(dialog,which)->
                            new AlertDialog.Builder(activity).setTitle(R.string.leave_final_title)
                                .setMessage(last?R.string.leave_final_last:R.string.leave_final_member)
                                .setNegativeButton(R.string.leave_cancel,null)
                                .setPositiveButton(R.string.leave_confirm,(second,choice)->{
                                    settings.setEnabled(false);
                                    new Thread(()->{
                                        try {
                                            org.json.JSONObject result=store.depart(logout,preview);
                                            activity.runOnUiThread(()->{
                                                if(activity.isFinishing()||activity.isDestroyed())return;
                                                if(result.optBoolean("cleanupPending"))Toast.makeText(activity,R.string.leave_cleanup_pending,Toast.LENGTH_LONG).show();
                                                activity.startActivity(new Intent(activity,LoginActivity.class)
                                                    .putExtra(LoginActivity.EXTRA_CHOOSE_JOURNEY,!logout)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                activity.finish();
                                            });
                                        }catch(RuntimeException error){showFailure(activity,settings,error);}
                                    }).start();
                                }).show()).show();
                });
            }catch(RuntimeException error){showFailure(activity,settings,error);}
        }).start();
    }
    private static void showFailure(Activity activity,View settings,RuntimeException error){
        activity.runOnUiThread(()->{
            if(activity.isFinishing()||activity.isDestroyed())return;
            settings.setEnabled(true);Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();
        });
    }
}
