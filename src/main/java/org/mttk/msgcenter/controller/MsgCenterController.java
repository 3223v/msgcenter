package org.mttk.msgcenter.controller;


import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Update;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgRecordModel;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.model.vo.ResponseEntity;
import org.mttk.msgcenter.service.MsgRecordService;
import org.mttk.msgcenter.service.SendMsgService;
import org.mttk.msgcenter.service.TemplateService;
import org.springframework.web.bind.annotation.*;

/**
 * 对外接口
 */
@RestController
@RequestMapping("/msg")
public class MsgCenterController {
    @Resource
    private TemplateService templateService;
    @Resource
    private SendMsgService sendMsgService;

    @Resource
    private MsgRecordService msgRecordService;

    @PostMapping("/create_template")
    public ResponseEntity<String> createTemplate(@RequestBody TemplateModel templateModel) {
        String templateId = templateService.CreateTemplate(templateModel);
        ResponseEntity<String> responseEntity = new ResponseEntity();
        responseEntity.setData(templateId);
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        return responseEntity;
    }

    @GetMapping("/get_template")
    public ResponseEntity<TemplateModel> getTemplate(@RequestParam("templateId") String templateId) {
        TemplateModel templateModel = templateService.GetTemplate(templateId);
        ResponseEntity<TemplateModel> responseEntity = new ResponseEntity();
        responseEntity.setData(templateModel);
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        return responseEntity;
    }

    @PostMapping("/update_template")
    public ResponseEntity<Void> updateTemplate(@RequestBody TemplateModel templateModel) {
        templateService.UpdateTemplate(templateModel);
        ResponseEntity<Void> responseEntity = new ResponseEntity();
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        return responseEntity;
    }

    @PostMapping("delete_template")
    public ResponseEntity<Void> deleteTemplate(@RequestParam("templateId") String templateId) {
        templateService.DeleteTemplate(templateId);
        ResponseEntity<Void> responseEntity = new ResponseEntity();
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        return responseEntity;
    }
    @PostMapping(value = "/send_msg")
    public ResponseEntity<String> send_msg(@RequestBody SendMsgReq sendMsgReq){
        String msgId = sendMsgService.SendMsg(sendMsgReq);
        ResponseEntity<String> responseEntity = new ResponseEntity();
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        responseEntity.setData(msgId);
        return responseEntity;
    }

    @GetMapping(value = "/get_msg_record")
    public ResponseEntity<MsgRecordModel> getMsgRecord(@RequestParam(value = "msgId") String msgId){
        MsgRecordModel msgRecordModel= msgRecordService.GetMsgRecordWithCache(msgId);
        ResponseEntity<MsgRecordModel> responseEntity = new ResponseEntity();
        responseEntity.setData(msgRecordModel);
        responseEntity.setMsg("success");
        responseEntity.setCode(200);
        return responseEntity;
    }
}
