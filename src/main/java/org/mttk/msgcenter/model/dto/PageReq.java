package org.mttk.msgcenter.model.dto;

import lombok.Data;

@Data
public class PageReq {
    private int pageNum = 1;
    private int pageSize = 10;
}
