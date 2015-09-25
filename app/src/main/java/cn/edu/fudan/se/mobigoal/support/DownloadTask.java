/**
 *
 */
package cn.edu.fudan.se.mobigoal.support;

import java.io.Serializable;

/**
 * 从服务器上下载goal model的任务
 *
 * @author whh
 */
public class DownloadTask implements Serializable {

    private String name;
    private String url;
    /**
     * true表示已经被下载，显示对号；false表示还没有被下载，显示下载按钮
     */
    private boolean isAlreadyDownload;

    public DownloadTask(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return true表示已经被下载，显示对号；false表示还没有被下载，显示下载按钮
     */
    public boolean isAlreadyDownload() {
        return isAlreadyDownload;
    }

    public void setAlreadyDownload(boolean isAlreadyDownload) {
        this.isAlreadyDownload = isAlreadyDownload;
    }


}
