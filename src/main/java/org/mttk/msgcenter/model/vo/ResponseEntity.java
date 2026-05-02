package org.mttk.msgcenter.model.vo;

import lombok.Data;

@Data
public class ResponseEntity<T> {
    private T data;
    private Integer code;
    private String msg;
}
