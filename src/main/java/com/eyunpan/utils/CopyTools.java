package com.eyunpan.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {

    public static <A,B> List<A> copyList(List<B> blist,Class<A> classz ){
        ArrayList<A> list = new ArrayList<>();
        for(B b:blist){
            A a=null;
            try {
                a = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            BeanUtils.copyProperties(b,a);
            list.add(a);
        }
        return list;
    }

    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }
}
