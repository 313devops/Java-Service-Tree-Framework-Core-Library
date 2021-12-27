package egovframework.com.ext.jstree.springiBatis.core.controller;

import com.google.common.collect.Maps;
import egovframework.com.ext.jstree.springiBatis.core.service.CoreService;
import egovframework.com.ext.jstree.springiBatis.core.util.Util_TitleChecker;
import egovframework.com.ext.jstree.springiBatis.core.validation.group.*;
import egovframework.com.ext.jstree.springiBatis.core.vo.ComprehensiveTree;
import egovframework.com.ext.jstree.springiBatis.core.vo.PaginatedComprehensiveTree;
import egovframework.com.ext.jstree.support.mvc.GenericAbstractController;
import egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping(value = { "/auth-anon/com/ext/jstree/springiBatis/core" })
public class CoreController extends GenericAbstractController{
	@Resource(name = "CoreService")
	CoreService coreService;
	
	@ResponseBody
	@RequestMapping(value="/getChildNode.do", method=RequestMethod.GET)
	public ModelAndView getChildNode(ComprehensiveTree comprehensiveTree, ModelMap model,
			HttpServletRequest request) throws Exception {
		if (comprehensiveTree.getC_id() <= 0) {
			throw new RuntimeException();
		}
		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", coreService.getChildNode(comprehensiveTree));
		return modelAndView;
	}
	
	@ResponseBody
	@RequestMapping(value="/getPaginatedChildNode.do", method=RequestMethod.GET)
	public ModelAndView getPaginatedChildNode(PaginatedComprehensiveTree paginatedComprehensiveTree, ModelMap model,
			HttpServletRequest request) throws Exception {
		if (paginatedComprehensiveTree.getC_id() <= 0) {
			throw new RuntimeException();
		}
		/** paging */
    	PaginationInfo paginationInfo = new PaginationInfo();
	    paginationInfo.setCurrentPageNo(paginatedComprehensiveTree.getPageIndex());
	    paginationInfo.setRecordCountPerPage(paginatedComprehensiveTree.getPageUnit());
	    paginationInfo.setPageSize(paginatedComprehensiveTree.getPageSize());
	    
	    paginatedComprehensiveTree.setFirstIndex(paginationInfo.getFirstRecordIndex());
	    paginatedComprehensiveTree.setLastIndex(paginationInfo.getLastRecordIndex());
	    paginatedComprehensiveTree.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
	    
	    List<PaginatedComprehensiveTree> resultChildNodes = coreService.getChildNode(paginatedComprehensiveTree);
	    paginationInfo.setTotalRecordCount(resultChildNodes.size());
	    
		ModelAndView modelAndView =  new ModelAndView("jsonView");
		HashMap<String, Object> resultMap = Maps.newHashMap();
		resultMap.put("paginationInfo", paginationInfo);
		resultMap.put("result", resultChildNodes);
		modelAndView.addObject("result", resultMap);
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/searchNode.do",method=RequestMethod.GET)
	public ModelAndView searchNode(ComprehensiveTree comprehensiveTree, ModelMap model, HttpServletRequest request)
			throws Exception {
		if (!StringUtils.hasText(request.getParameter("searchString"))) {
			throw new RuntimeException();
		}

		comprehensiveTree.setSearchStr(request.getParameter("searchString"));

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", coreService.searchNode(comprehensiveTree));
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/addNode.do",method=RequestMethod.POST)
	public ModelAndView addNode(@Validated(value = AddNode.class) ComprehensiveTree comprehensiveTree,
			BindingResult bindingResult, ModelMap model) throws Exception {
		if (bindingResult.hasErrors())
			throw new RuntimeException();

		comprehensiveTree.setC_title(Util_TitleChecker.StringReplace(comprehensiveTree.getC_title()));

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", coreService.addNode(comprehensiveTree));
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/removeNode.do",method=RequestMethod.POST)
	public ModelAndView removeNode(@Validated(value = RemoveNode.class) ComprehensiveTree comprehensiveTree,
			BindingResult bindingResult, ModelMap model) throws Exception {
		if (bindingResult.hasErrors())
			throw new RuntimeException();

		comprehensiveTree.setStatus(coreService.removeNode(comprehensiveTree));

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", comprehensiveTree);
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/alterNode.do",method=RequestMethod.POST)
	public ModelAndView alterNode(@Validated(value = AlterNode.class) ComprehensiveTree comprehensiveTree,
			BindingResult bindingResult, ModelMap model) throws Exception {
		if (bindingResult.hasErrors())
			throw new RuntimeException();

		comprehensiveTree.setC_title(Util_TitleChecker.StringReplace(comprehensiveTree.getC_title()));
		
		comprehensiveTree.setStatus(coreService.alterNode(comprehensiveTree));

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", comprehensiveTree);
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/alterNodeType.do",method=RequestMethod.POST)
	public ModelAndView alterNodeType(@Validated(value = AlterNodeType.class) ComprehensiveTree comprehensiveTree,
			BindingResult bindingResult, ModelMap model) throws Exception {
		if (bindingResult.hasErrors())
			throw new RuntimeException();

		coreService.alterNodeType(comprehensiveTree);

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", comprehensiveTree);
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/moveNode.do",method=RequestMethod.POST)
	public ModelAndView moveNode(@Validated(value = MoveNode.class) ComprehensiveTree comprehensiveTree,
			BindingResult bindingResult, ModelMap model, HttpServletRequest request) throws Exception {
		if (bindingResult.hasErrors())
			throw new RuntimeException();

		coreService.moveNode(comprehensiveTree, request);

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", comprehensiveTree);
		return modelAndView;
	}

	@ResponseBody
	@RequestMapping(value="/analyzeNode.do",method=RequestMethod.GET)
	public ModelAndView getChildNode(ModelMap model) {
		model.addAttribute("analyzeResult", "");

		ModelAndView modelAndView =  new ModelAndView("jsonView");
		modelAndView.addObject("result", "ture");
		return modelAndView;
	}

}