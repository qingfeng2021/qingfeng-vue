package com.qingfeng.system.controller;

import com.qingfeng.base.controller.BaseController;
import com.qingfeng.framework.jwt.util.JwtUtils;
import com.qingfeng.system.service.DictionaryService;
import com.qingfeng.system.service.UserService;
import com.qingfeng.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.sf.jxls.transformer.XLSTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Title: DictionaryController
 * @ProjectName com.qingfeng
 * @Description: 字典Controller层
 * @author anxingtao
 * @date 2020-9-22 22:45
 */
@Api(tags = "字典信息接口")
@Controller
@RequestMapping(value = "/system/dictionary")
public class DictionaryController extends BaseController {

	@Autowired
	private DictionaryService dictionaryService;
	@Autowired
	private UserService userService;


	/** 
	 * @Description: findListPage 
	 * @Param: [page, request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:47 
	 */
	@ApiOperation("查询数据分页列表接口")
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
		List<PageData> list = dictionaryService.findListPage(page);
		int num = dictionaryService.findListSize(page);
		Json json = new Json();
		json.setMsg("获取数据成功。");
		json.setCode(0);
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
     * @Date: 2020-9-22 22:47 
     */
	@ApiOperation("查询数据列表接口")
	@RequestMapping(value = "/findList", method = RequestMethod.POST)
	public void findList(@RequestHeader HttpHeaders headers,@RequestBody PageData pd, HttpServletResponse response) throws IOException  {
    	List<PageData> list = dictionaryService.findList(pd);
        Json json = new Json();
        json.setMsg("获取数据成功。");
        json.setData(list);
        json.setSuccess(true);
        this.writeJson(response,json);
    }

	/** 
	 * @Description: findInfo 
	 * @Param: [map, request] 
	 * @return: java.lang.String 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:47 
	 */
	@ApiOperation("查询数据详情接口")
	@RequestMapping(value = "/findInfo", method = RequestMethod.GET)
	public void findInfo(@RequestParam String id, HttpServletResponse response) throws Exception {
		PageData pd = new PageData();
		pd.put("id",id);
		PageData p = dictionaryService.findInfo(pd);
		Json json = new Json();
		json.setMsg("获取数据成功。");
		json.setData(p);
		json.setSuccess(true);
		this.writeJson(response,json);
	}



	/** 
	 * @Description: saveOrUpdate
	 * @Param: [request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:47 
	 */
	@ApiOperation("保存或更新数据接口")
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
			PageData p = dictionaryService.findInfo(pd);
			if(pd.get("code").equals(p.get("code"))){
				pd.put("update_time", time);
				pd.put("update_user",user_id);
				dictionaryService.update(pd);
				json.setSuccess(true);
				json.setMsg("操作成功。");
			}else{
				PageData param = new PageData();
				param.put("codes", Arrays.asList(pd.get("code").toString().split(",")));
				List<PageData> list = dictionaryService.findList(param);
				if(list.size()>0){
					String msg = "字典编码："+pd.get("code")+"已经存在。";
					json.setSuccess(false);
					json.setMsg(msg);
				}else{
					pd.put("update_time", time);
					pd.put("update_user",user_id);
					dictionaryService.update(pd);
					json.setSuccess(true);
					json.setMsg("操作成功。");
					json.setSuccess(true);
				}
			}
			json.setMsg("数据编辑成功。");
		}else{
			PageData param = new PageData();
			param.put("codes", Arrays.asList(pd.get("code").toString().split(",")));
			List<PageData> list = dictionaryService.findList(param);
			if(list.size()>0){
				String msg = "字典编码："+pd.get("code")+"已经存在。";
				json.setSuccess(false);
				json.setMsg(msg);
			}else {
				//主键id
				String id = GuidUtil.getUuid();
				pd.put("id", id);
				pd.put("create_time", time);
				pd.put("type","1");
				pd.put("status","0");
				//处理数据权限
				pd.put("create_user",user_id);
				pd.put("create_organize",organize_id);
				pd.put("dic_cascade", pd.get("dic_cascade").toString()+id+"_");
				pd.put("level_num",Integer.parseInt(pd.get("level_num").toString())+1);
				dictionaryService.save(pd);
				json.setMsg("数据添加成功。");
				json.setSuccess(true);
			}
			json.setMsg("操作成功。");
		}
		this.writeJson(response,json);
	}


	/** 
	 * @Description: update 
	 * @Param: [request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:48 
	 */
	@ApiOperation("更新数据接口")
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public void update(@RequestHeader HttpHeaders headers, @RequestBody PageData pd, HttpServletResponse response) throws IOException  {
		//处理数据权限
		String token = headers.get("access-token").get(0);
		String user_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[0];
		String time = DateTimeUtil.getDateTimeStr();
		PageData p = dictionaryService.findInfo(pd);
		Json json = new Json();
		if(pd.get("code").equals(p.get("code"))){
			pd.put("update_time", time);
			pd.put("update_user",user_id);
			dictionaryService.update(pd);
			json.setSuccess(true);
			json.setMsg("操作成功。");
		}else{
			PageData param = new PageData();
			param.put("codes", Arrays.asList(pd.get("code").toString().split(",")));
			List<PageData> list = dictionaryService.findList(param);
			if(list.size()>0){
				String msg = "字典编码："+pd.get("code")+"已经存在。";
				json.setSuccess(false);
				json.setMsg(msg);
			}else{
				pd.put("update_time", time);
				pd.put("update_user",user_id);
				dictionaryService.update(pd);
				json.setSuccess(true);
				json.setMsg("操作成功。");
			}
		}
		this.writeJson(response,json);
	}


	/** 
	 * @Description: del 
	 * @Param: [request, response] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:48 
	 */
	@ApiOperation("删除数据接口")
	@RequestMapping(value = "/del", method = RequestMethod.GET)
	public void del(@RequestHeader HttpHeaders headers, @RequestParam String ids, HttpServletResponse response) throws IOException  {
		PageData pd = new PageData();
		String[] del_ids = ids.split(",");
		dictionaryService.del(del_ids);
		Json json = new Json();
		json.setSuccess(true);
		json.setMsg("操作成功。");
		this.writeJson(response,json);
	}


	/** 
	 * @Description: updateStatus
	 * @Param: [request, response] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-22 22:52
	 */
	@ApiOperation("更新数据状态接口")
	@RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
	public void updateStatus(@RequestHeader HttpHeaders headers,@RequestBody PageData pd, HttpServletResponse response) throws IOException  {
		String time = DateTimeUtil.getDateTimeStr();
		pd.put("update_time", time);
		dictionaryService.update(pd);

		Json json = new Json();
		json.setSuccess(true);
		json.setFlag("操作成功。");
		this.writeJson(response,json);
	}


	/** 
	 * @Description: exportData
	 * @Param: [request, response, session] 
	 * @return: void 
	 * @Author: anxingtao
	 * @Date: 2020-9-24 0:18 
	 */
	@ApiOperation("导出字典信息接口")
	@RequestMapping(value = "/exportData", method = RequestMethod.GET)
	public void exportData(@RequestHeader HttpHeaders headers,@RequestParam String short_name,
						   @RequestParam String name,@RequestParam String status,
						   @RequestParam String parent_id,@RequestParam String token,
						   HttpServletResponse response,HttpSession session) throws Exception {
		PageData pd = new PageData();
		pd.put("short_name",short_name);
		pd.put("name",name);
		pd.put("status",status);
		pd.put("parent_id",parent_id);
		//处理数据权限
		String user_id = JwtUtils.validateJWT(token).getClaims().getId().split(":")[0];
		String organizeId = JwtUtils.validateJWT(token).getClaims().getId().split(":")[1];
		pd.put("auth_user",user_id);
		pd.put("auth_organize",organizeId);
		pd.put("user_id",user_id);
		PageData orgPd = userService.findUserOrganizeInfo(pd);
		if(Verify.verifyIsNotNull(orgPd)){
			if(Verify.verifyIsNotNull(orgPd.get("authOrgIds"))){
				pd.put("auth_organize_ids", Arrays.asList(orgPd.get("authOrgIds").toString().split(",")));
			}else{
				pd.put("auth_organize_ids",new ArrayList<String>());
			}
		}
		List<PageData> list = dictionaryService.findList(pd);
		Map<String, Object> beans = new HashMap<String, Object>();
		beans.put("obj", pd);
		beans.put("list", list);
		String tempPath = "";
		String toFile = "";
		String path = Thread.currentThread().getContextClassLoader().getResource("").getPath()+"templates";
		tempPath = path+"/excelExport/system_dictionary.xls";
		toFile = path+"/excelExport/temporary/system_dictionary.xls";
		XLSTransformer transformer = new XLSTransformer();
		transformer.transformXLS(tempPath, beans, toFile);
		FileUtil.downFile(response, toFile, "青锋系统字典基础信息_" + DateTimeUtil.getDateTimeStr() + ".xls");
		File file = new File(toFile);
		file.delete();
		file.deleteOnExit();
	}


	/**
	 * @Description: findInfoJson
	 * @Param: [request, response]
	 * @return: void
	 * @Author: anxingtao
	 * @Date: 2020-10-13 22:52
	 */
	@ApiOperation("查询数据信息接口")
	@RequestMapping(value = "/findInfoJson", method = RequestMethod.GET)
	public void findInfoJson(HttpServletRequest request, HttpServletResponse response) throws IOException  {
		PageData pd = new PageData(request);
		pd.put("ids",Arrays.asList(pd.get("id").toString().split(",")));
		List<PageData> list = dictionaryService.findList(pd);
		Json json = new Json();
		json.setMsg("获取数据成功。");
		json.setData(list);
		json.setSuccess(true);
		this.writeJson(response,json);
	}

}
