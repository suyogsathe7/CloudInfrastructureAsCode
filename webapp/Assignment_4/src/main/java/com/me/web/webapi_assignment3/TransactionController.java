package com.me.web.webapi_assignment3;

import com.me.web.dao.AttachmentDao;
import com.me.web.dao.TransactionDao;
import com.me.web.dao.UserDao;
import com.me.web.pojo.Attachment;
import com.me.web.pojo.Transaction;
import com.me.web.pojo.User;
import com.me.web.service.AmazonClient;
import com.me.web.service.LogHelper;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class TransactionController {

    @Value("${storagepath}")
    String storagePath;

    @Value("${run}")
    String run;

    @Autowired
    private AmazonClient amazonClient;

    @Autowired
    private StatsDClient statsDClient;


    LogHelper logger = new LogHelper();

    @RequestMapping(value = "transaction", method = RequestMethod.POST)
    public HashMap<String, Object> saveTransaction(HttpServletRequest req, TransactionDao txDao, UserDao userDao, AttachmentDao attachmentDao, @RequestPart(value="file", required = false) MultipartFile file) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.api.post");
        logger.logInfoEntry("Save transaction API initiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                logger.logInfoEntry("Unauthorized access");
                map.put("Code",401);
                map.put("Description","Unauthorized");
                UserDao.close();
                return map;
            }else{
                String description = req.getParameter("description");
                String merchant = req.getParameter("merchant");
                String amount = req.getParameter("amount");
                String date = req.getParameter("date");
                String category = req.getParameter("category");
                Transaction tx = new Transaction();
                    tx.setDescription(description);
                    tx.setMerchant(merchant);
                    tx.setAmount(Double.parseDouble(amount));
                    tx.setDate(date);
                    tx.setCategory(category);
                    tx.setUser(user);
                    if(txDao.insertTransaction(tx)==2){
                        if(file!=null && !file.isEmpty()){
                        Attachment attachment = new Attachment();
                        attachment.setTransaction(tx);

                            if (attachmentDao.saveAttachment(attachment) == 2) {
                                tx.addAttachment(attachment);
                                if(run.equalsIgnoreCase("dev")){
                                    File destFile = new File(storagePath+attachment.getId());
                                    if(file!=null && !file.isEmpty()){
                                        file.transferTo(destFile);
                                        attachment.setUrl(storagePath+attachment.getId());
                                        attachmentDao.editAttachments(attachment);
                                    }
                                }
                             else if(run.equalsIgnoreCase("aws"))
                            {
                                String fileUrl = this.amazonClient.uploadFile(file, String.valueOf(attachment.getId()));
                                attachment.setUrl(fileUrl);
                                attachmentDao.editAttachments(attachment);
                            }
                        }
                        }
                        logger.logInfoEntry("Successfully saved");
                        map.put("Description",tx);
                        map.put("Code",200);
                        txDao.close();
                        userDao.close();
                        attachmentDao.close();
                        return map;
                    }
            }
        }
        logger.logInfoEntry("Save user transacton Unauthorized Access");
        map.put("Code",401);map.put("Description","Unauthorized");
        txDao.close();
        userDao.close();
        attachmentDao.close();
        return map;

    }


    @RequestMapping(value = "transaction/{id}/attachments", method = RequestMethod.POST)
    public HashMap<String, Object> saveAttachments(@PathVariable("id") UUID id, HttpServletRequest req, TransactionDao txDao, AttachmentDao attachmentDao, UserDao userDao, @RequestPart("file") MultipartFile file) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.api.post");
        logger.logInfoEntry("transaction/{id}/attachments initiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                logger.logInfoEntry("Unauthorized access");
                map.put("Code",401);
                map.put("Description","Unauthorized");
                //txDao.close();
                //userDao.close();
                //attachmentDao.close();
                return map;
            }else{
                UUID txId = id;
                if(txDao.authorizeUser(txId,user) == 2) {
                    Transaction tx = txDao.getTransactionById(txId);
                    Attachment attachment = new Attachment();
                    attachment.setTransaction(tx);
                    if (attachmentDao.saveAttachment(attachment) == 2) {
                        map.put("Description", attachment);
                        map.put("Code", 200);

//                        MultipartFile file = req.getHeader("file");
                        if(run.equalsIgnoreCase("dev")){
                        File destFile = new File(storagePath+attachment.getId());
                        if(file!=null && !file.isEmpty()){
                            file.transferTo(destFile);
                            attachment.setUrl(storagePath+attachment.getId());
                            attachmentDao.editAttachments(attachment);
                        }}
                        else if(run.equalsIgnoreCase("aws")){
                                String fileUrl = this.amazonClient.uploadFile(file, String.valueOf(attachment.getId()));
                                   attachment.setUrl(fileUrl);
                                   attachmentDao.editAttachments(attachment);
                        }
                        logger.logInfoEntry("transaction/{id}/attachments successfuly saved");
                        //txDao.close();
                        //userDao.close();
                        //attachmentDao.close();
                        return map;
                    }
                    else{
                        logger.logInfoEntry("transaction/{id}/attachments unsuccessful");
                        map.put("Description", "Attachment unsuccessful");
                        map.put("Code", 500);
                        //txDao.close();
                        //userDao.close();
                        //attachmentDao.close();
                        return map;
                    }
                }
                else{
                    logger.logInfoEntry("transaction/{id}/attachments unauthorized access");
                    map.put("Code", 401);
                    map.put("Description", "Unauthorized");
                    //txDao.close();
                    //userDao.close();
                    //attachmentDao.close();
                    return map;
                }
            }
        }
        logger.logInfoEntry("transaction/{id}/attachments unauthorized access");
        map.put("Code",401);map.put("Description","Unauthorized");
        //txDao.close();
        //userDao.close();
        //attachmentDao.close();
        return map;

    }


    @RequestMapping(value="transaction/{id}", method = RequestMethod.DELETE)
    public HashMap<String, Object> deleteTransaction(@PathVariable("id") UUID id, HttpServletRequest req, TransactionDao txDao, UserDao userDao, AttachmentDao attachmentDao) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.api.delete");
        logger.logInfoEntry("transaction/{id}/ delete initiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                logger.logInfoEntry("transaction/{id}/ delete Unauthorized");
                map.put("Code",401);
                map.put("Description","Unauthorized");
                return map;
            }else{
                UUID txId = id;
                if(txId != null){
                    if(txDao.authorizeUser(txId,user) == 2) {
                        List<Attachment> list = attachmentDao.getAllAttachments(id);
                        if (txDao.deleteTransaction(txId) == 2) {
                            if(!list.isEmpty()){
                                for(Attachment attachment : list){
                                    if(run.equalsIgnoreCase("dev")){
                                File destFile = new File(attachment.getUrl());
                                if(destFile.exists()){
                                    destFile.delete();
                                }}else if(run.equalsIgnoreCase("aws")){
                                        amazonClient.deleteFileFromS3Bucket(attachment.getUrl());
                                    }
                                }
                            }
                            logger.logInfoEntry("transaction/{id}/ delete successful");
                            map.put("Code", 200);
                            map.put("Description", "Successfully Deleted");
                            return map;
                        } else {
                            logger.logInfoEntry("transaction/{id}/ delete Bad request");
                            map.put("Code", 400);
                            map.put("Description", "Bad Request");
                            return map;
                        }
                    }
                    else{
                        map.put("Code", 401);
                        map.put("Description", "Unauthorized");
                        return map;
                    }
                }else{
                    map.put("Code",204);
                    map.put("Description","No Content");
                    return map;
                }
            }
        }
        map.put("Code",401);
        map.put("Description","Unauthorized");
        return map;
    }

    @RequestMapping(value = "transaction/{id}/attachments/{idAttachments}", method = RequestMethod.DELETE)
    public HashMap<String, Object> deleteAttachments(@PathVariable("id") UUID id, @PathVariable("idAttachments") UUID idAt,HttpServletRequest req, TransactionDao txDao, AttachmentDao attachmentDao, UserDao userDao) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.id.api.delete");
        logger.logInfoEntry("transaction/{id}/attachments/{idAttachments} Bad request");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                logger.logInfoEntry("transaction/{id}/ delete Bad request");
                map.put("Code",401);
                map.put("Description","Unauthorized");
                return map;
            }else{
                UUID txId = id;
                if(txDao.authorizeUser(txId,user) == 2) {
                    Transaction tx = txDao.getTransactionById(txId);
                    Attachment attachment = new Attachment();
                    attachment.setTransaction(tx);
                    attachment.setId(idAt);
                    Attachment at = attachmentDao.getAttachmentById(idAt);
                    if (attachmentDao.deleteAttachment(attachment) == 2) {
                        map.put("Code", 204);
                        map.put("Description", "Attachment Successfully Deleted");


                       if(run.equalsIgnoreCase("dev")){
                        File destFile = new File(at.getUrl());
                        if(destFile.exists()){
                            destFile.delete();
                        }
                       }else if(run.equalsIgnoreCase("aws")){
                           amazonClient.deleteFileFromS3Bucket(at.getUrl());
                       }
                        return map;
                    }
                    else{
                        map.put("Code", 500);
                        map.put("Description", "Attachment not Deleted");
                        return map;
                    }
                }
                else{
                    map.put("Code", 401);
                    map.put("Description", "Unauthorized");
                    return map;
                }
            }
        }
        map.put("Code",401);map.put("Description","Unauthorized");
        return map;

    }

    @RequestMapping(value="transaction/{id}", method = RequestMethod.PUT)
    public HashMap<String, Object> updateTransaction(@PathVariable("id") UUID id, HttpServletRequest req, TransactionDao txDao, UserDao userDao) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.api.put");
        logger.logInfoEntry("transaction/{id}/ put intiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                map.put("Code",401);
                map.put("Description","Unauthorized");
                return map;
            }else{
                UUID txId = id;
                if(txDao.authorizeUser(txId,user) == 2) {
                    Transaction tx = txDao.getTransactionById(txId);
                    if (tx == null) {
                        map.put("Code", 400);
                        map.put("Description", "Bad Request");
                        return map;
                    }
                    String description = req.getParameter("description") == null ? tx.getDescription() : req.getParameter("description");
                    String merchant = req.getParameter("merchant") == null ? tx.getMerchant() : req.getParameter("merchant");
                    String amount = req.getParameter("amount") == null ? String.valueOf(tx.getAmount()) : req.getParameter("amount");
                    String date = req.getParameter("date") == null ? tx.getDate() : req.getParameter("date");
                    String category = req.getParameter("category") == null ? tx.getCategory() : req.getParameter("category");

                    tx.setId(txId);
                    tx.setDescription(description);
                    tx.setMerchant(merchant);
                    tx.setAmount(Double.parseDouble(amount));
                    tx.setDate(date);
                    tx.setCategory(category);
                    tx.setUser(user);
                    if (txDao.editTransaction(tx) == 2) {
                        logger.logInfoEntry("transaction/{id}/ put intiated");
                        map.put("Code", 201);
                        map.put("Description", "Created");
                        map.put("Transaction", tx);
                        return map;
                    } else {
                        map.put("Code", 400);
                        map.put("Description", "Bad Request");
                        return map;
                    }
                }
                else{
                    map.put("Code", 401);
                    map.put("Description", "Unauthorized");
                    return map;
                }
            }
        }
        map.put("Code",401);
        map.put("Description","Unauthorized");
        return map;
    }

    @RequestMapping(value="transaction/{id}/attachments/{aid}", method = RequestMethod.PUT)
    public HashMap<String, Object> updateAttachmentTransaction(@PathVariable("id") UUID id,@PathVariable("aid") UUID aid, HttpServletRequest req, TransactionDao txDao, UserDao userDao, AttachmentDao attachmentDao,@RequestPart("file")MultipartFile file) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.id.api.put");
        logger.logInfoEntry("transaction/{id}/attachments/{aid} put intiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                map.put("Code",401);
                map.put("Description","Unauthorized");
                return map;
            }else{
                UUID txId = id;
                if(txDao.authorizeUser(txId,user) == 2) {
                    Transaction tx = txDao.getTransactionById(txId);
                    if (tx == null) {
                        map.put("Code", 400);
                        map.put("Description", "Bad Request");
                        return map;
                    }
                    Attachment attachment = attachmentDao.getAttachmentById(aid);

                    if(attachment != null) {
                        //tx.addAttachment(attachment);
                        if(run.equalsIgnoreCase("dev")){
                            File destFile = new File(storagePath+attachment.getId());
                            if(file!=null && !file.isEmpty()){
                                if(destFile.exists()){
                                    destFile.delete();
                                }
                                file.transferTo(destFile);
                                attachment.setUrl(storagePath+attachment.getId());
                                //attachmentDao.editAttachments(attachment);
                            }}
                        else if(run.equalsIgnoreCase("aws")){
                            amazonClient.deleteFileFromS3Bucket(attachment.getUrl());
                            String fileUrl = this.amazonClient.uploadFile(file, String.valueOf(attachment.getId()));
                            attachment.setUrl(fileUrl);
                            //attachmentDao.editAttachments(attachment);
                        }
                        logger.logInfoEntry("transaction/{id}/attachments/{aid} put file updated");
                            map.put("Code", 200);
                            map.put("Description", "File updated");

                            return map;
                    }
                }
                else{
                    map.put("Code", 401);
                    map.put("Description", "Unauthorized");
                    return map;
                }
            }
        }
        map.put("Code",401);
        map.put("Description","Unauthorized");
        return map;
    }


    @RequestMapping(value="transaction", method = RequestMethod.GET)
    public HashMap<String, Object> getAllTransaction(HttpServletRequest req, TransactionDao txDao, UserDao userDao) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.api.get");
        logger.logInfoEntry("transaction get initiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        HashMap<String, Object> map = new HashMap<>();
        User user = null;
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                map.put("Code",401);
                map.put("Description", "Unauthorized");
                return map;
            }else{
                List<Transaction> list = txDao.getAllTransaction(user.getId());
                if(list.isEmpty()){
                    logger.logInfoEntry("transaction get successful");
                    map.put("Code",200);
                    map.put("Description", "No transaction found");
                    //return "{message:'No transaction found'}";
                    return map;
                }else{
                    map.put("Code",200);
                    map.put("Description","OK");
                    map.put("Transaction", list);
                    return map;
                }
            }
        }
        map.put("Code",401);
        map.put("Description", "Unauthorized");
        return map;
}

    @RequestMapping(value = "transaction/{id}/attachments", method = RequestMethod.GET)
    public HashMap<String, Object> getAllAttachments(@PathVariable("id") UUID id, HttpServletRequest req, TransactionDao txDao, AttachmentDao attachmentDao, UserDao userDao) throws  Exception{
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.api.get");
        logger.logInfoEntry("transaction/{id}/attachments get initiated");
        String headers = req.getHeader(HttpHeaders.AUTHORIZATION);
        User user = null;
        HashMap<String, Object> map = new HashMap<>();
        if(headers != null) {
            String base64Credentials = headers.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);
            user = userDao.verifyUser(values[0], values[1]);
            if (user == null || user.getUsername().isEmpty()) {
                map.put("Code",401);
                map.put("Description","Unauthorized");
                return map;
            }else{

                UUID txId = id;
                if(txDao.authorizeUser(txId,user) == 2) {
                    List<Attachment> list = attachmentDao.getAllAttachments(id);
                    if (list.isEmpty()) {
                        map.put("Code", 500);
                        map.put("Description", "No attachment found");

                        return map;
                    }
                    else{
                        logger.logInfoEntry("transaction/{id}/attachments get successful");
                        map.put("Code", 200);
                        map.put("Description", "OK");
                        map.put("Attachment", list);
                        return map;
                    }
                }
                else{
                    map.put("Code", 401);
                    map.put("Description", "Unauthorized");

                    return map;
                }
            }
        }
        map.put("Code",401);map.put("Description","Unauthorized");
        return map;
    }
}
