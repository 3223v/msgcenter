package org.mttk.msgcenter.service;

import org.mttk.msgcenter.model.dto.PageReq;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.model.vo.PageResult;

import java.util.List;

public interface TemplateService {
    String CreateTemplate(TemplateModel templateModel);

    void DeleteTemplate(String templateID);

    void UpdateTemplate(TemplateModel templateModel);

    TemplateModel GetTemplate(String templateID);

    PageResult<TemplateModel> GetTemplateList(PageReq pageReq);
}
