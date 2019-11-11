package com.yanzhenjie.album.impl;

import android.widget.CompoundButton;

/**
 * <p>作者：hsicen  2019/11/7 15:08
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：选择框点击监听
 */
public interface OnCheckedClickListener {

    /**
     * Compound button is clicked.
     *
     * @param button   view.
     * @param position the position in the list.
     */
    void onCheckedClick(CompoundButton button, int position);
}