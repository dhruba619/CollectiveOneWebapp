package org.collectiveone.web.controllers.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectiveone.model.Project;
import org.collectiveone.services.DbServicesImp;
import org.collectiveone.services.Filters;
import org.collectiveone.services.ProjectDtoListRes;
import org.collectiveone.web.dto.ProjectContributorsDto;
import org.collectiveone.web.dto.ProjectDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/rest/projects")
public class ProjectsController {
	
	@Autowired
	DbServicesImp dbServices;
	
	@RequestMapping(value="/get/{projectName}", method = RequestMethod.POST)
	public @ResponseBody ProjectDto get(@PathVariable("projectName") String projectName) {
		return dbServices.projectGetDto(projectName);
	}
	
	@RequestMapping(value="/getContributors/{projectName}", method = RequestMethod.POST)
	public @ResponseBody ProjectContributorsDto getContributors(@PathVariable("projectName") String projectName) {
			
		Project project = dbServices.projectGet(projectName);
		
		ProjectContributorsDto projectContributorsDto = new ProjectContributorsDto();
		projectContributorsDto.setUsernamesAndPps(dbServices.projectContributorsAndPpsGet(project.getId()));
		projectContributorsDto.setPpsTot(project.getPpsTot());
		
		return projectContributorsDto;
	}
	
	@RequestMapping("/getNamesList")
	public Map<String,Object> getNamesList() {
		
		List<String> projectList = dbServices.projectGetList();
		
		Map<String,Object> map = new HashMap<>();
		map.put("projectList", projectList);
		
		return map;
	}
	
	@RequestMapping(value="/getList", method = RequestMethod.POST)
	public @ResponseBody Map<String,Object> getList(@RequestBody Filters filters) {
		if(filters.getPage() == 0) filters.setPage(1);
		if(filters.getNperpage() == 0) filters.setNperpage(15);
		
		ProjectDtoListRes projectsDtosRes = dbServices.projectDtoGetFiltered(filters);
		
		List<ProjectDto> projectDtos = projectsDtosRes.getProjectDtos();
		int[] resSet = projectsDtosRes.getResSet();
		
		Map<String,Object> map = new HashMap<>();
		
		map.put("projectDtos", projectDtos);
		map.put("resSet", resSet);
		
		return map;
	}
	
}
