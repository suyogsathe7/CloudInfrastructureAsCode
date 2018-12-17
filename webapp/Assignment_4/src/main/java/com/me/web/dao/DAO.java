package com.me.web.dao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.core.io.ClassPathResource;

public class DAO {

    private static final Logger log = Logger.getAnonymousLogger();
    Properties prop = new Properties();
    //private static final ThreadLocal<Session> sessionThread = new ThreadLocal<Session>();
    private static SessionFactory sessionFactory; //= new Configuration().configure().addProperties(prop).buildSessionFactory();
    private static Session session = null;
    protected DAO() throws IOException {
        prop.load(new ClassPathResource("hibernate.properties").getInputStream());
        sessionFactory = new Configuration().configure().addProperties(prop).buildSessionFactory();
    }

    public static Session getSession()
    {
        // = (Session) DAO.sessionThread.get();

        if (session == null)
        {
            session = sessionFactory.openSession();
            //DAO.sessionThread.set(session);
        }
        return session;
    }

    protected void begin() {
        getSession().beginTransaction();
    }

    protected void commit() {
        getSession().getTransaction().commit();
    }

    protected void rollback() {
        try {
            getSession().getTransaction().rollback();
        } catch (HibernateException e) {
            log.log(Level.WARNING, "Cannot rollback", e);
        }
        try {
            getSession().close();
        } catch (HibernateException e) {
            log.log(Level.WARNING, "Cannot close", e);
        }
        //DAO.sessionThread.set(null);
    }

    public static void close() {
        //getSession().close();
        getSession().disconnect();
        //DAO.sessionThread.remove();
    }

}
