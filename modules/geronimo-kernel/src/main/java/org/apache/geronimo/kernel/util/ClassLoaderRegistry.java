package org.apache.geronimo.kernel.util;

import java.util.List;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class ClassLoaderRegistry {
    private static List<WeakReference> list = new ArrayList<WeakReference>();
    public static List getList(){
        List<ClassLoader> ret = new ArrayList<ClassLoader>();
        for(int i=0;i<list.size();i++)
            if(list.get(i) != null)ret.add((ClassLoader)list.get(i).get());
            else
                list.remove(i);
        return ret;
    }
    public static boolean add(ClassLoader cloader){
        if(contains(cloader))
            return false;
        return list.add(new WeakReference<ClassLoader>(cloader));
    } 
    public static boolean contains(ClassLoader cloader){
        for(int i=0;i<list.size();i++){
            WeakReference wk = list.get(i);
            if(wk.get() == null)list.remove(i);
            else if(wk.get().equals(cloader))
                return true;            
        }
        return false;
    }
    public static boolean remove(ClassLoader cloader){
        boolean result = false;
        for(int i=0;i<list.size();i++){
            WeakReference wk = list.get(i);
            if(wk.get() == null)list.remove(i);
            else if(wk.get().equals(cloader)){
                list.remove(i);
                result = true;
            }
        }
        return result;
    } 
}
