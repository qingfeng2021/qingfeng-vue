package com.qingfeng.gencode.controller;

import com.qingfeng.base.controller.BaseController;
import com.qingfeng.common.service.UploadService;
import com.qingfeng.framework.jwt.util.JwtUtils;
import com.qingfeng.gencode.service.MycontentService;
import com.qingfeng.system.service.UserService;
import com.qingfeng.util.*;
import com.qingfeng.util.upload.ParaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Title: MycontentController
 * @ProjectName com.qingfeng
 * @Description: Controller层
 * @author anxingtao
 * @date 2020-9-22 22:45
 */
@Controller
@RequestMapping(value = "/gencode/mycontent")
public class MycontentController extends BaseController {

	@Autowired
	private MycontentService mycontentService;
	@Autowired
	public UploadService uploadService;
	@Autowired
	private UserService userService;


	/** 
	 * @Description: findListPage 
	 * @Param: [page, request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:51 
	 */
	@RequestMapping(value = "/findListPage", method = RequestMethod.POST)
	public void findListPage(@RequestHeader HttpHeaders headers, @RequestBody PageData pd, HttpServletResponse response) throws IOException {
		Page page = new Page();
		//处理数据权限
		String  token = headers.get("access-token").get(0);
		String user_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[0];
		String organize_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[1];
		pd.put("auth_user",user_id);
		pd.put("auth_organize",organize_id);
		pd.put("user_id",user_id);
		PageData orgPd = userService.findUserOrganizeInfo(pd);
		if(Verify.verifyIsNotNull(orgPd.get("authOrgIds"))){
			pd.put("auth_organize_ids",Arrays.asList(orgPd.get("authOrgIds").toString().split(",")));
		}else{
			pd.put("auth_organize_ids",new ArrayList<String>());
		}
		//处理分页
		if(Verify.verifyIsNotNull(pd.get("page"))){
		page.setIndex(Integer.parseInt(pd.get("page").toString()));
		}else{
		page.setIndex(1);
		}
		if(Verify.verifyIsNotNull(pd.get("limit"))){
		page.setShowCount(Integer.parseInt(pd.get("limit").toString()));
		}else{
		page.setShowCount(10);
		}
		page.setPd(pd);
		List<PageData> list = mycontentService.findListPage(page);
		for(PageData p:list){
			//查询简介附件信息
			PageData filePd = new PageData();
			filePd.put("idList",Arrays.asList(p.get("intro").toString().split(",")));
			List<PageData> introFileList = uploadService.findFileList(filePd);
			p.put("introFileList",introFileList);
		}
		int num = mycontentService.findListSize(page);
		Json json = new Json();
		json.setMsg("获取数据成功。");
		json.setCount(num);
		json.setData(list);
		json.setSuccess(true);
		this.writeJson(response,json);
	}

    /** 
     * @Description: findList 
     * @Param: [request, response] 
     * @return: void 
     * @Author: anxingtao
     * @Date: 2020-9-22 22:51 
     */
        @RequestMapping(value = "/findList", method = RequestMethod.POST)
        public void findList(@RequestHeader HttpHeaders headers,@RequestBody PageData pd, HttpServletResponse response) throws IOException  {
    	List<PageData> list = mycontentService.findList(pd);
        Json json = new Json();
        json.setMsg("获取数据成功。");
        json.setData(list);
        json.setSuccess(true);
        this.writeJson(response,json);
    }

	/** 
	 * @Description: save 
	 * @Param: [request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:51 
	 */
	@RequestMapping(value = "/saveOrUpdate", method = RequestMethod.POST)
	public void save(@RequestHeader HttpHeaders headers, @RequestBody PageData pd, HttpServletResponse response) throws IOException  {
		Json json = new Json();
		//处理数据权限
		String  token = headers.get("access-token").get(0);
		System.out.println("findUserInfo###########TOKEN:"+token);
		String user_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[0];
		String organize_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[1];
		String time = DateTimeUtil.getDateTimeStr();
        if(Verify.verifyIsNotNull(pd.get("id"))){
            pd.put("update_time", time);
            pd.put("update_user",user_id);
            int num = mycontentService.update(pd);
            if(num==1){
                    //处理简介附件信息-更新信息主要意义在于可删选删除垃圾图片
                    PageData filePd = new PageData();
                    filePd.put("obj_id",pd.get("id"));
                    filePd.put("update_time", time);
                    filePd.put("update_user", user_id);
                    String file_intro[] = pd.get("intro").toString().split(",");
                    for (String file_intro_id:file_intro) {
                    filePd.put("id", file_intro_id);
                    uploadService.updateFile(filePd);
                    }
            }
            json.setSuccess(true);
            json.setMsg("操作成功。");
		}else{
            //主键id
            String id = GuidUtil.getUuid();
            pd.put("id", id);
            pd.put("create_time", time);
            pd.put("status","1");
            //处理数据权限
            pd.put("create_user",user_id);
            pd.put("create_organize",organize_id);

            int num = mycontentService.save(pd);
            if(num==1){
                    //处理简介附件信息-更新信息主要意义在于可删选删除垃圾图片
                    PageData filePd = new PageData();
                    filePd.put("obj_id",id);
                    filePd.put("update_time", time);
                    filePd.put("update_user", user_id);
                    String file_intro[] = pd.get("intro").toString().split(",");
                    for (String file_intro_id:file_intro) {
                    filePd.put("id", file_intro_id);
                    uploadService.updateFile(filePd);
                    }
            }
            json.setSuccess(true);
            json.setMsg("操作成功。");
		}
		this.writeJson(response,json);
	}


	/**
	 * @Description: del 
	 * @Param: [request, response] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:51 
	 */
	@RequestMapping(value = "/del", method = RequestMethod.GET)
	public void del(@RequestHeader HttpHeaders headers, @RequestParam String ids, HttpServletResponse response) throws IOException  {
		PageData pd = new PageData();
		String[] del_ids = ids.split(",");
        //删除主表附件信息
        for (String id:del_ids) {
			pd.put("obj_id",id);
			List<PageData> fileList = uploadService.findFileList(pd);
			for (PageData fPd:fileList) {
				//查询信息
				File pathFile = new File(ParaUtil.localName+fPd.getString("file_path"));
				pathFile.delete();
				pathFile.deleteOnExit();
				uploadService.delFile(fPd);
			}
        }
		mycontentService.del(del_ids);
		Json json = new Json();
		json.setSuccess(true);
		json.setMsg("操作成功。");
		this.writeJson(response,json);
	}

}
