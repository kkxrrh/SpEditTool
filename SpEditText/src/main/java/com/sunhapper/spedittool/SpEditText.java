package com.sunhapper.spedittool;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

public class SpEditText extends AppCompatEditText {

  private static String TAG = "SpEditText";
  private KeyReactListener mKeyReactListener;
  private char[] reactKeys;

  public SpEditText(Context context) {
    super(context);
    init();
  }

  public SpEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (isInEditMode()) {
      return;
    }
    init();
  }


  public void setReactKeys(String reactKeys) {
    this.reactKeys = reactKeys.toCharArray();
  }

  /**
   * 设置文本变化监听
   * 设置删除事件监听
   */
  private void init() {
    addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        for (Character character : reactKeys) {
          if (count == 1 && !TextUtils.isEmpty(charSequence)) {
            char mentionChar = charSequence.toString().charAt(start);
            if (character.equals(mentionChar) && mKeyReactListener != null) {
              handKeyReactEvent(character);
              return;
            }
          }
        }
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });

    setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
          return onDeleteEvent();
        }

        return false;
      }
    });
  }

  private void handKeyReactEvent(Character character) {
    mKeyReactListener.onKeyReact(character.toString());
  }

  private boolean onDeleteEvent() {
    int selectionStart = getSelectionStart();
    int selectionEnd = getSelectionEnd();
    if (selectionEnd != selectionStart) {
      return false;
    }
    SpData[] spDatas = getSpDatas();
    for (int i = 0; i < spDatas.length; i++) {
      SpData spData = spDatas[i];
      int rangeStart = spData.start;
      if (selectionStart == spData.end) {
        getEditableText().delete(rangeStart, selectionEnd);
        return true;
      }

    }
    return false;
  }


  public SpData[] getSpDatas() {
    Editable editable = getText();
    SpData[] spanneds = editable.getSpans(0, getText().length(), SpData.class);
    if (spanneds != null && spanneds.length > 0) {
      for (SpData spData : spanneds) {
        int start = editable.getSpanStart(spData);
        int end = editable.getSpanEnd(spData);
        spData.setEnd(end);
        spData.setStart(start);
      }
      sortSpans(spanneds, 0, spanneds.length - 1);
      return spanneds;
    } else {
      return new SpData[]{};
    }


  }

  private void sortSpans(SpData[] spDatas, int left, int right) {
    if (left >= right) {
      return;
    }
    int i = left;
    int j = right;
    SpData keySpan = spDatas[left];
    int key = spDatas[left].start;
    while (i < j) {
      while (i < j && key <= spDatas[j].start) {
        j--;
      }
      spDatas[i] = spDatas[j];
      while (i < j && key >= spDatas[i].start) {
        i++;
      }

      spDatas[j] = spDatas[i];
    }

    spDatas[i] = keySpan;
    sortSpans(spDatas, left, i - 1);
    sortSpans(spDatas, i + 1, right);

  }

  /**
   * 监听光标位置
   */
  @Override
  protected void onSelectionChanged(int selStart, int selEnd) {
    super.onSelectionChanged(selStart, selEnd);
    SpData[] spDatas = getSpDatas();
    for (int i = 0; i < spDatas.length; i++) {
      SpData spData = spDatas[i];
      int startPosition = spData.start;
      int endPosition = spData.end;
      if (changeSelection(selStart, selEnd, startPosition, endPosition, false)) {
        return;
      }
    }
  }

  /**
   * 不让光标进入特殊字符串内部
   *
   * @param selStart 光标起点
   * @param selEnd 光标终点
   * @param startPosition 特殊字符串起点
   * @param endPosition 特殊字符串终点
   * @param toEnd 将光标放起点还是终点,暂时没用上,都是放起点
   */
  private boolean changeSelection(int selStart, int selEnd, int startPosition, int endPosition,
      boolean toEnd) {
    boolean hasChange = false;
    if (selStart == selEnd) {
      if (startPosition != -1 && startPosition < selStart && selStart < endPosition) {
        if (toEnd) {
          setSelection(endPosition);
        } else {
          setSelection(startPosition);
        }
        hasChange = true;
      }
    } else {
      if (startPosition != -1 && startPosition < selStart && selStart < endPosition) {
        if (toEnd) {
          setSelection(endPosition, selEnd);
        } else {
          setSelection(startPosition, selEnd);
        }

        hasChange = true;
      }
      if (endPosition != -1 && startPosition < selEnd && selEnd < endPosition) {
        setSelection(selStart, endPosition);
        hasChange = true;
      }
    }
    return hasChange;
  }


  /**
   * 插入特殊字符串
   *
   * @param showContent 特殊字符串显示在文本框中的内容
   * @param rollBack 是否往前删除一个字符，因为@的时候可能留了一个字符在输入框里
   * @param customData 特殊字符串的数据结构
   * @param customSpan 特殊字符串的样式
   */
  public void insertSpecialStr(String showContent, boolean rollBack, Object customData,
      Object customSpan) {
    if (TextUtils.isEmpty(showContent)) {
      return;
    }
    int index = getSelectionStart();
    Editable editable = getText();
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editable);
    SpData spData = new SpData();
    spData.setShowContent(showContent);
    spData.setCustomData(customData);
    SpannableString spannableString = new SpannableString(showContent);
    spannableString
        .setSpan(spData, 0, spannableString.length(),
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (customSpan != null) {
      spannableString
          .setSpan(customSpan, 0, spannableString.length(),
              SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    if (rollBack) {
      spannableStringBuilder.delete(index - 1, index);
      index--;
    }
    spannableStringBuilder.insert(index, spannableString);
    setText(spannableStringBuilder);
    setSelection(index + spannableString.length());
  }


  public interface KeyReactListener {

    void onKeyReact(String key);
  }


  public void setmKeyReactListener(KeyReactListener mKeyReactListener) {
    this.mKeyReactListener = mKeyReactListener;
  }

  public class SpData {

    /**
     * EditText中显示的内容
     */
    private String showContent;
    /**
     * 特殊内容的数据结构
     */
    private Object customData;
    private int start;
    private int end;


    public int getStart() {
      return start;
    }

    private void setStart(int start) {
      this.start = start;
    }

    public int getEnd() {
      return end;
    }

    private void setEnd(int end) {
      this.end = end;
    }

    public Object getCustomData() {
      return customData;
    }

    private void setCustomData(Object customData) {
      this.customData = customData;
    }

    public String getShowContent() {
      return showContent;
    }

    private void setShowContent(String showContent) {
      this.showContent = showContent;
    }

    @Override
    public String toString() {
      return "SpData{" +
          "showContent='" + showContent + '\'' +
          ", customData=" + customData +
          ", start=" + start +
          ", end=" + end +
          '}';
    }
  }


}