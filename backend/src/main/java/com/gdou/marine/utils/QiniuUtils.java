package com.gdou.marine.utils;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description 七牛云设置
 */
public class QiniuUtils {
    public  static String accessKey = "REDACTED_QINIU_ACCESS_KEY";
    public  static String secretKey = "REDACTED_QINIU_SECRET_KEY";
    public  static String bucket = "demo-flnyxx";//空间名称

    /**
     * 七牛云 CDN 加速域名（请替换为你的实际域名）
     */
    public static String domainOfBucket = "http://tgig77s29.hn-bkt.clouddn.com";

    /**
     * 获取文件的完整访问 URL
     */
    public static String getFileUrl(String fileName) {
        return domainOfBucket + "/" + fileName;
    }

    /**
     * 字节数组方式上传到七牛云
     * @param bytes 文件字节数组
     * @param fileName 保存到七牛云的文件名
     * @return 上传成功后的文件访问 URL
     */
    public static String upload2Qiniu(byte[] bytes, String fileName){
        //构造一个带指定 Region 对象的配置类（和原方法保持一致）
        Configuration cfg = new Configuration(Region.region2());
        UploadManager uploadManager = new UploadManager(cfg);
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);

        try {
            // 核心改动：第一个参数直接传入字节数组
            Response response = uploadManager.put(bytes, fileName, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            return getFileUrl(fileName);
        } catch (QiniuException ex) {
            Response r = ex.response;
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
            throw new RuntimeException("上传文件到七牛云失败: " + fileName, ex);
        }
    }

    //删除文件
    public static void deleteFileFromQiniu(String fileName){
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        String key = fileName;
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(bucket, key);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }
    }
}
