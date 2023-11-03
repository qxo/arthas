package com.taobao.arthas.core.util.matcher;

import java.util.Collection;

/**
 *
 */
public final class ListMatcher<T> implements Matcher<T> {

     private final Collection<T> list;

     public ListMatcher(Collection<T> list) {
        this.list = list;
     }
    
    /**
     * @param target
     * @return
     */
    @Override
    public boolean matching(T target) {
        return list.contains(target);
    }

}