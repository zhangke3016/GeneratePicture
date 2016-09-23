# GeneratePicture 选取页面内容生成精美分享图片
>Select the page content generated picture.

>仿简书选取页面内容生成图片效果。


![GeneratePicture](http://upload-images.jianshu.io/upload_images/2157910-e0e045940d391968.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

实现这个效果，首先要弄明白几个问题：
>**一、**如何获取选取的网页内容
>**二、**获取的网页内容如何加载显示

![GeneratePicture](http://upload-images.jianshu.io/upload_images/2157910-ce0dec5de176c5ed.gif?imageMogr2/auto-orient/strip)


##一、如何获取选取的网页内容

获取选取的网页内容，通过Java来获取选取的网页内容很困难，而实现效果又必须要得到选取的网页内容，我们可以转换下思路，既然通过Java层不容易得到那通过JavaScript是不是要容易点呢，之后的实现确定这个思路是正确的，JavaScript很容易获取选取的网页内容。

那我们的思路就是：当用户点击生成图片分享按钮后，我们调用JavaScript方法获取选取的网页内容同时回调Java的获取内容方法，将获取的网页内容回传到Java层，我们就可以拿到网页的内容了。
简单看下代码：
```
mWebView.addJavascriptInterface(new WebAppInterface(onGetDataListener), "JSInterface");

public void getSelectedData(WebView webView) {
        String js = "(function getSelectedText() {" +
                "var txt;" +
                "if (window.getSelection) {" +
                "var range=window.getSelection().getRangeAt(0);" +
                "var container = window.document.createElement('div');" +
                "container.appendChild(range.cloneContents());" +
                "txt = container.innerHTML;" +
                "} else if (window.document.getSelection) {" +
                "var range=window.getSelection().getRangeAt(0);" +
                "var container = window.document.createElement('div');" +
                "container.appendChild(range.cloneContents());" +
                "txt = container.innerHTML;" +
                "} else if (window.document.selection) {" +
                "txt = window.document.selection.createRange().htmlText;" +
                "}" +
                "JSInterface.getText(txt);" +
                "})()";

        // calling the js function
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("javascript:" + js, null);
        } else {
            webView.loadUrl("javascript:" + js);
        }
        webView.clearFocus();
    }
 static class WebAppInterface {
        WebViewHelper.OnGetDataListener onGetDataListener;
        WebAppInterface(WebViewHelper.OnGetDataListener onGetDataListener) {
            this.onGetDataListener = onGetDataListener;
        }
        @JavascriptInterface
        public void getText(String text) {
            onGetDataListener.getDataListener(text);
        }
    }
    public interface OnGetDataListener{
        void getDataListener(String text);
    }
```
上面的实现思路就是当我们要获取选取的网页内容时，给WebView注入一段自己写的JavaScript脚本，这段JavaScript代码的含义就是获取当前页面选取的内容包含html标签，调用`JSInterface.getText(txt)`方法将内容回传给Java的`getText(String text)`方法，我们设置`onGetDataListener.getDataListener(text)`回调方法，由需要的地方调用获取内容。

##二、获取的网页内容如何加载显示

我们已经获取到了网页内容，按道理其实调用`TextView的setText(Html.fromHtml())`这个方法就可以显示我们选取的效果，但考虑到美观性以及截图保存功能、图片的正常显示，我选取用WebView来加载获取的网页内容。

 这里我是这样处理的：首先在本地`assets`文件夹下创建一个html页面，在页面里加载基本的显示内容并添加css标签修饰加载的内容，当获取到网页内容时，用JavaScript动态替换本地html页面指定的对应标签内容为获取的网页内容，并在本地html页面里对显示内容进行修饰。

看下代码：
```
webView.loadUrl("file:///android_asset/generate_pic.html");

public void changeDay(String strData,String userInfo,String userName,String other) {
        if(userInfo == null)
            userInfo ="";
        if(strData == null)
            strData ="";
        if(userName == null)
            userName ="";
        if(other == null)
            other ="";
        strData+="<br /><br />\n" +
                "\t\t<span style=\"font-size: small;color: gray;line-height:150%;\">"+userInfo+"</span>\n" +
                "\t\t<br /><br />\n" +
                "\t\t<hr style=\"margin: auto;border:0;background-color:gray;height:1px;\"/>\n" +
                "\t\t<br />\n" +
                "\t\t<p style=\"color: orangered;font-size: x-small;text-align: center;letter-spacing: 0.5px;\">由<strong>"+userName+"</strong>发送 "+other+"</p>";
        webView.loadUrl("javascript:changeContent(\"" + strData.replace("\n", "\\n").replace("\"", "\\\"").replace("'", "\\'") + "\")");
        webView.setBackgroundColor(Color.WHITE);
    }
```
白色和黑色不同的显示效果实现可以在`changeDay`方法里改变css样式来实现，比较简单。

但这里出现了一个问题：**当选取的页面内容有图片且图片是以相对路径显示的时候就加载不到图片了。**

在这种情况下图片是相对路径也就是在本地对应的相对路径下找，本地肯定是找不到的，图片也就显示不出来。
为了让图片正常显示出来，在选取内容页面调用`onLoadResource`方法对加载的资源进行判断，将图片路径保存下来，因为既然选取页面图片可以显示处理，说明路径是http路径，可以显示图片。
看下代码：
```
 mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onLoadResource(WebView view, String url) {
                //Log.e("TAG","url :"+url);
                if(url.toLowerCase().contains(".jpg")
                        ||url.toLowerCase().contains(".png")
                        ||url.toLowerCase().contains(".gif")){
                    mlistPath.add(url);
                }
                super.onLoadResource(view, url);
            }
```
当显示选取内容页面显示时动态修改显示的图片路径，让图片显示出来：

```
 webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //view.loadUrl(url);
                return true;
            }
            public WebResourceResponse shouldInterceptRequest(WebView view,  String url) {
                WebResourceResponse response = null;
                for (String path:WebViewHelper.getInstance().getAllListPath()){
                    if (path.toLowerCase().contains(url.replace("file://","").toLowerCase())){
                        try {
                            response = new WebResourceResponse("image/png", "UTF-8", new URL(path).openStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return response;
            }
        });
```

这样，我们的图片就可以显示出来啦！
最后，实现我们的截图保存功能，看下代码：
```
 /**
     * 截屏
     *
     * @return
     */
    public Bitmap getScreen() {
        Bitmap bmp = Bitmap.createBitmap(webView.getWidth(), 1, Bitmap.Config.ARGB_8888);
        int rowBytes = bmp.getRowBytes();
        bmp = null;

        if (rowBytes*webView.getHeight()>=getAvailMemory()){
            return null;
        }
        bmp = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        webView.draw(canvas);
        return bmp;
    }
   private long getAvailMemory() {
        return Runtime.getRuntime().maxMemory();
    }
```

这里需要对保存的图片大小做下判断，防止创建图片过大OOM。

到这里，基本功能就已经实现了。把图片分享给好友吧~
