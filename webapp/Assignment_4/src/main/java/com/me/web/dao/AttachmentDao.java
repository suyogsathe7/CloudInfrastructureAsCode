package com.me.web.dao;

import com.me.web.pojo.Attachment;
import com.me.web.pojo.Transaction;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttachmentDao extends DAO{

    public AttachmentDao() throws IOException {
        super();
    }
    public int saveAttachment(Attachment attachment) throws Exception{
        try{
            begin();
            getSession().save(attachment);
//            getSession().flush();
            commit();
//            getSession().clear();
            close();
        return 2;
        }
        catch(HibernateException e){
            rollback();
            throw new Exception("Attachment not saved: "+e.getMessage());
        }
    }


    public Attachment getAttachmentById(UUID id) throws Exception{
        try {
            char flag = ' ';
            if(!getSession().getTransaction().isActive())
            {
                begin();
                flag = 'X';
            }
            Attachment attachment = (Attachment)getSession().get(Attachment.class, id);
            if(flag=='X'){
                commit();
//                getSession().flush();
                getSession().clear();
            }
            if(attachment!=null){
                return attachment;
            }else{
                return null;
            }
        }catch(HibernateException e){
            rollback();
            throw new Exception("Attachment not found: "+e.getMessage());
        }

    }

    public int deleteAttachment(Attachment attachment) throws Exception{
        try{
            begin();
            if(attachment!=null){
                getSession().delete(attachment);
//                getSession().flush();
                commit();
//                getSession().clear();
                close();
                return 2;
            }
                return 1;
        }
        catch(HibernateException e){
            rollback();
            throw  new Exception("Attachment not Deleted: "+e.getMessage());
        }
    }

//    public int storeAttachment(Attachment file){
//        try {
//            MultipartFile fileInMemory = file.getFile();
//            java.io.File destFile = new java.io.File(url, fileInMemory.getOriginalFilename());
//            fileInMemory.transferTo(destFile);
//            return 2;
//        }
//        catch(Exception ex){
//            System.out.println("Exception Message:" + ex.getMessage());
//        }
//        return 0;
//    }

    /*
    public ArrayList<Attachment> getAttachmentByTransaction(int id) throws Exception{
        List<Attachment> attachmentList = new ArrayList<>();
        try{
            begin();
            Query q = getSession().createQuery("from Attachment where transaction_id = :id");
            q.setInteger("id", id);
            attachmentList = q.getResultList();
            commit();
        }catch (HibernateException e){
            rollback();
            throw new Exception("Exception while getting transactions"+e.getMessage());
        }
        return (ArrayList<Attachment>) attachmentList;
    }
    */

    public List<Attachment> getAllAttachments(UUID id)throws Exception{
        try{
            List<Attachment> list;
            begin();
            Query q = getSession().createQuery("from Attachment where transaction_id = :id");
            q.setParameter("id", id);
            list = q.getResultList();
//            getSession().flush();
            commit();
//            getSession().clear();
            return list;

        }catch (HibernateException e){
            rollback();
            throw new Exception("Exception while getting attachments"+e.getMessage());
        }
    }

    public int editAttachments(Attachment at)throws Exception{
        try{
            begin();
            getSession().saveOrUpdate(at);
//            getSession().flush();
            commit();
//            getSession().clear();
            close();
            return 2;
        }catch (HibernateException e){
            rollback();
            throw new Exception("Exception while updating attachment"+e.getMessage());
        }

    }
}
