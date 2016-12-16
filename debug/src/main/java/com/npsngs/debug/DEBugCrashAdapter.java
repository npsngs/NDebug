package com.npsngs.debug;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class DEBugCrashAdapter extends Adapter<DEBugCrashAdapter.FileEntry> {
    DEBugCrashAdapter(Context mContext) {
        super(mContext);
    }

    void loadData() {
        File errLog = new File(DEBug.getCrashSaveDir());
        if (!errLog.exists() || !errLog.isDirectory()) {
            return;
        }

        List<File> files = new ArrayList<>();
        File[] fs = errLog.listFiles(filter);
        if(null != fs && fs.length > 0){
            Collections.addAll(files, fs);
            Collections.sort(files, comparator);
            final List<FileEntry> fileEntries = new ArrayList<>(files.size());
            for(File f:files){
                FileEntry fileEntry = new FileEntry();
                fileEntry.file = f;
                fileEntry.date = f.getName().replace("crash_", "").replace(".txt", "");
                fileEntries.add(fileEntry);
            }
            setData(fileEntries);

            if(fileEntries.isEmpty()){
                return;
            }

            new Thread(){
                @Override
                public void run() {
                    Handler handler = new Handler(Looper.getMainLooper()){
                        @Override
                        public void handleMessage(Message msg) {
                            DEBugCrashAdapter.this.notifyDataSetInvalidated();
                        }
                    };

                    Pattern pattern = Pattern.compile("\\.([\\w]+?)(:|$)");

                    for(FileEntry fileEntry:fileEntries){
                        try {
                            FileReader fr = new FileReader(fileEntry.file);
                            BufferedReader br = new BufferedReader(fr);
                            String topLine = br.readLine();
                            Matcher matcher = pattern.matcher(topLine);
                            if(matcher.find()){
                                fileEntry.brief = matcher.group(1);
                            }

                            if(!TextUtils.isEmpty(fileEntry.brief)){
                                handler.sendEmptyMessage(0);
                            }

                            br.close();
                            fr.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        CrashHolder crashHolder;
        if(v == null){
            TextView tv = new TextView(getContext());
            tv.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setTextSize(12f);
            tv.setClickable(false);
            tv.setGravity(Gravity.LEFT);
            int padding = DEBugUtils.dp2px(getContext(), 10);
            tv.setPadding(padding, padding, padding, padding);
            tv.setTextColor(0xffff2200);
            tv.setSingleLine(true);
            tv.setClickable(false);
            crashHolder = new CrashHolder(tv);
            tv.setTag(crashHolder);
        }else{
            crashHolder = (CrashHolder) v.getTag();
        }

        crashHolder.bind(position);
        return crashHolder.tv;
    }


    class CrashHolder implements View.OnClickListener, View.OnLongClickListener{
        TextView tv;
        int position;
        CrashHolder(TextView tv) {
            this.tv = tv;
            tv.setOnClickListener(this);
            tv.setOnLongClickListener(this);
        }

        void bind(int position){
            this.position = position;
            FileEntry fileEntry = getItem(position);
            if(!TextUtils.isEmpty(fileEntry.brief)){
                tv.setText(String.format("%s  [%s]",  fileEntry.date, fileEntry.brief));
            }else{
                tv.setText(fileEntry.file.getName());
            }
        }

        @Override
        public void onClick(View v) {
            if(null != onShowParseText){
                String crashLog = getErrStr(position);
                onShowParseText.showParsedText(crashLog, 0xffff2200);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            String crashLog = getErrStr(position);
            DEBugUtils.sendText(getContext(), crashLog);
            return true;
        }
    }


    private final FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith("crash_");
        }
    };

    private final Comparator<File> comparator = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            return (int) (rhs.lastModified() - lhs.lastModified());
        }
    };

    private DEBugPopup.OnShowParseText onShowParseText;
    void setOnShowParseText(DEBugPopup.OnShowParseText onShowParseText) {
        this.onShowParseText = onShowParseText;
    }

    private String getErrStr(int pos){
        try{
            FileEntry fileEntry = getItem(pos);
            FileReader fr = new FileReader(fileEntry.file);
            char[] buffer = new char[1024];
            StringBuilder builder = new StringBuilder();
            int ret;
            while(-1 != (ret = fr.read(buffer))){
                String s = new String(buffer, 0, ret);
                builder.append(s);
            }
            fr.close();
            return builder.toString();
        }catch(Exception e){
            e.printStackTrace();
        }

        return "";
    }


    class FileEntry{
        File file;
        String brief = null;
        String date = null;
    }
}
