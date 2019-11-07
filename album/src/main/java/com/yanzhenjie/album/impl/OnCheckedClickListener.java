/*
 * Copyright 2018 Yan Zhenjie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yanzhenjie.album.impl;

import com.yanzhenjie.album.widget.AlbumCheckBox;

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
    void onCheckedClick(AlbumCheckBox button, int position);
}