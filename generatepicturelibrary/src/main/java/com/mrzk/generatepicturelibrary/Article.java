package com.mrzk.generatepicturelibrary;

import java.io.Serializable;

/**
 * 选取内容实体类
 * @author zhangke3016
 */
public class Article implements Serializable{
    //内容
    public String strData;
    //标题
    public String strTitle;

    public Article(String strData, String strTitle) {
        this.strData = strData;
        this.strTitle = strTitle;
    }
}
