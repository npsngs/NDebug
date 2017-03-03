package com.forthe.xlog.panel.filters;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.forthe.xlog.R;
import com.forthe.xlog.core.Container;
import com.forthe.xlog.frame.PanelBase;

import java.util.regex.Pattern;

public abstract class PatternEditPanel extends PanelBase implements TextView.OnEditorActionListener, TextWatcher {
    private EditText editText;
    private CheckBox cb_ignore_case;
    private String findRegex;
    private Pattern findPattern;
    private FilterContainer filterContainer;
    PatternEditPanel(int mode,FilterContainer filterContainer) {
        super(mode);
        this.filterContainer = filterContainer;
    }

    @Override
    protected void onResume(Container container) {
        super.onResume(container);
        isCompiled = false;
    }

    @Override
    protected View onCreateView(Context context, ViewGroup parent) {
        View root = View.inflate(context, R.layout.forthe_xlog_pattern_editor,null);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(0, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.RIGHT_OF, -1024);
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, -1024);
        lp.addRule(RelativeLayout.ALIGN_TOP, -1024);
        root.setLayoutParams(lp);

        findRegex = filterContainer.getPattern();
        editText = (EditText) root.findViewById(R.id.et);
        editText.setOnEditorActionListener(this);
        editText.addTextChangedListener(this);
        if (!TextUtils.isEmpty(findRegex)) {
            editText.setText(findRegex);
        }

        cb_ignore_case = (CheckBox) root.findViewById(R.id.cb_ignore_case);
        cb_ignore_case.setChecked(filterContainer.isIgnoreCase());
        cb_ignore_case.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterContainer.setIgnoreCase(isChecked);
                isCompiled = false;
            }
        });
        return root;
    }

    protected abstract void onFilterAction(String patternStr, Pattern pattern);

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(EditorInfo.IME_ACTION_SEARCH == actionId){
            if(compile()){
                dismiss();
            }
            return true;
        }
        return false;
    }

    public boolean compile(){
        CharSequence s = editText.getText();
        if(TextUtils.isEmpty(s)){
            findRegex = null;
            findPattern = null;
        }else{
            findRegex = s.toString();
            try{
                findPattern = Pattern.compile(String.format("%s%s",filterContainer.isIgnoreCase()?"(?i)":"(?-i)",s));
            }catch (Exception e){
                findPattern = null;
            }
        }
        isCompiled = true;
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        onFilterAction(findRegex, findPattern);
        return null != findPattern;
    }


    private boolean isCompiled = false;
    public boolean isCompiled() {
        return isCompiled;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        isCompiled = false;
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
