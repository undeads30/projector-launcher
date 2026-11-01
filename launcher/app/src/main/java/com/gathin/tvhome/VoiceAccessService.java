package com.gathin.tvhome;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
public class VoiceAccessService extends AccessibilityService {
    private static VoiceAccessService current;
    @Override protected void onServiceConnected(){current=this;}
    static boolean action(int action){return current!=null&&current.performGlobalAction(action);}
    static String foreground() {
        return VoiceSearchQuery.destination(activePackage());
    }
    static String activePackage() {
        if(current==null)return "";
        android.view.accessibility.AccessibilityNodeInfo root=current.getRootInActiveWindow();
        if(root==null)return "";
        try{return String.valueOf(root.getPackageName());}
        finally{root.recycle();}
    }
    static boolean setKinoQuery(String query) {
        if(current==null)return false;
        android.view.accessibility.AccessibilityNodeInfo root=current.getRootInActiveWindow();
        if(root==null)return false;
        try {
            if(!"mov.film".contentEquals(root.getPackageName()))return false;
            return fill(root,query);
        } finally {root.recycle();}
    }
    private static boolean fill(android.view.accessibility.AccessibilityNodeInfo node,String query) {
        if(node.isEditable()&&node.isVisibleToUser()&&"android.widget.EditText".contentEquals(node.getClassName())
                &&(same(node.getHintText(),"Что ищем?")||same(node.getText(),"Что ищем?"))) {
            android.os.Bundle args=new android.os.Bundle();
            args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,query);
            return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args);
        }
        for(int i=0;i<node.getChildCount();i++){
            android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null){try{if(fill(child,query))return true;}finally{child.recycle();}}
        }
        return false;
    }
    private static boolean same(CharSequence value,String expected){return value!=null&&expected.contentEquals(value);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        CharSequence pkg=event.getPackageName();
        if(pkg!=null)VoiceSearchRouter.windowChanged(pkg.toString());
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(current==this)current=null;super.onDestroy();}
}
