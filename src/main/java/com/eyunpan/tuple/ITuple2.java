package com.eyunpan.tuple;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ITuple2<A,B> {
    private A a;
    private B b;

    public static <A,B> ITuple2 valueOf(A a,B b){
        return new ITuple2(a,b);
    }
}
