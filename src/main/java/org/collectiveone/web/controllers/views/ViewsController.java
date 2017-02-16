package org.collectiveone.web.controllers.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.websocket.server.PathParam;

import org.collectiveone.model.GoalState;
import org.collectiveone.model.User;
import org.collectiveone.services.AppMailServiceHeroku;
import org.collectiveone.services.DbServicesImp;
import org.collectiveone.web.dto.CbtionDto;
import org.collectiveone.web.dto.DecisionDtoCreate;
import org.collectiveone.web.dto.DecisionDtoFull;
import org.collectiveone.web.dto.GoalDto;
import org.collectiveone.web.dto.InvRequest;
import org.collectiveone.web.dto.ProjectDto;
import org.collectiveone.web.dto.ProjectNewDto;
import org.collectiveone.web.dto.SignupRequest;
import org.collectiveone.web.dto.UsernameAndPps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.ullink.slack.simpleslackapi.SlackSession;

@Controller
@RequestMapping("/views")
public class ViewsController {
	
	@Autowired
	DbServicesImp dbServices;
	
	@Autowired
    private AppMailServiceHeroku mailService;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private SlackSession slackSession;
	
	
	
	@RequestMapping("/userPageR/{username}")
	public String userPage(@PathVariable("username") String username, Model model) {
		model.addAttribute("username",username);
		return "views/userPage";
	}
	
	@RequestMapping("/activityListPageR")
	public String activityList(Model model) {
		return "views/activityListPage";
	}
	
	@RequestMapping("/cbtionListPageR")
	public String cbtionListPage(Model model) {
		return "views/cbtionListPage";
	}
	
	@RequestMapping("/goalListPageR")
	public String goalListPageR(Model model) {
		return "views/goalListPage";
	}
	
	@RequestMapping("/cbtionPageR/{id}")
	public String cbtionPage(@PathVariable(value="id") Long id, Model model) {
		CbtionDto ctionDto = dbServices.cbtionGetDto(id);  
		
		model.addAttribute("cbtionTitle",ctionDto.getTitle());
		model.addAttribute("cbtionId",ctionDto.getId());
		model.addAttribute("cbtionDescription",ctionDto.getDescription());
		
		return "views/cbtionPage";
	}
	
	@RequestMapping("/goalPageR/{goalId}")
	public String goalPageId(@PathVariable("goalId") Long goalId, Model model) {
		GoalDto goalDto = dbServices.goalGetDto(goalId);  
		
		model.addAttribute("goalTag",goalDto.getGoalTag());
		model.addAttribute("projectName",goalDto.getProjectName());
		model.addAttribute("goalId",goalDto.getId());
		model.addAttribute("goalDescription",goalDto.getDescription());
		
		return "views/goalPage";
	}
	
	@RequestMapping("/goalPageR")
	public String goalPage(@PathParam("goalTag") String goalTag, @PathParam("projectName") String projectName, Model model) {
		GoalDto goalDto = dbServices.goalGetDto(goalTag,projectName);  
		
		model.addAttribute("goalTag",goalDto.getGoalTag());
		model.addAttribute("projectName",goalDto.getProjectName());
		model.addAttribute("goalId",goalDto.getId());
		model.addAttribute("goalDescription",goalDto.getDescription());
		
		return "views/goalPage";
	}
	
	@RequestMapping("/slackPageR")
	public String slackPage(Model model) {
		model.addAttribute("invRequest",new InvRequest());
		return "views/slackPage";
	}
	
	@RequestMapping("/helpPageR")
	public String helpPage(Model model) {
		return "views/helpPage";
	}
	
	@RequestMapping("/slackInvSubmit")
	public String slackInvSubmit(@Valid InvRequest invRequest, Model model) throws IOException {
		
		slackSession.inviteUser(invRequest.getEmail(), "", true);
		
		String subject = "Slack invitation request";
        String body = "Slack invitation sent to "+invRequest.getEmail();
        
        mailService.sendMail(
        		env.getProperty("collectiveone.webapp.admin-email"),
        		subject, 
        		body);
        
        model.addAttribute("message","An invitation has been sent to you at "+invRequest.getEmail()+"");
        
        return "views/slackPage";
	}
	
	@RequestMapping("/signupRequestPageR")
	public String signupRequestPage(Model model) {
		model.addAttribute("signupRequest",new SignupRequest());
		return "views/signUpRequestPage";
	}
	
	@RequestMapping("/signupRequestSubmit")
	public String signupRequestSubmit(@Valid SignupRequest signupRequest, Model model, final HttpServletRequest request) throws IOException {
		
		User referral = dbServices.userGet(signupRequest.getReferral());
		
		if(referral != null) {
			if(referral.getIsReferrer() != null) {			
				if(referral.getIsReferrer()) {
					String token = UUID.randomUUID().toString();
					
					dbServices.authorizedEmailAdd(signupRequest.getEmail(), referral.getId(), token);
					
					String authUrl = getAppUrl(request)+"/views/authorizeSignup?email="+signupRequest.getEmail()+"&token="+token;
					
					String subject = "Signup invitation request";
			        String body = "Request to signup sent by "+signupRequest.getEmail()+"."
			        				+"\n\nCollectiveOne wants to keep one real person behind each user."
			        				+ "\n\nIf you know this is the case for this request, please authorize it by visiting this link:"
			        				+ authUrl
			        				+"\n\nComments:"+signupRequest.getComments();
			        
			        mailService.sendMail(
			        		referral.getEmail(),
			        		subject, 
			        		body);
			        
			        model.addAttribute("message","Thanks, your request has been received "+signupRequest.getEmail()+"."+
			        		" An email has been sent to '"+referral.getUsername()+"' with a link to authorize your signup.");
			        return "views/signUpRequestPage";
				} else {
					model.addAttribute("message","Referral not found");
					return "views/signUpRequestPage";
				}
			} else {
				model.addAttribute("message","Referral not found");
				return "views/signUpRequestPage";
			}
		} else {
			model.addAttribute("message","Referral not found");
	        return "views/signUpRequestPage";
		}
	}
	
	@RequestMapping("/authorizeSignup")
    public String passwordRecovery(final Locale locale, final Model model, @RequestParam("email") String email, @RequestParam("token") String token, final HttpServletRequest request) throws IOException {
        if(dbServices.authorizedEmailValidate(email, token)) {
        	
        	String subject = "Signup request accepted";
	        String body = "Request to signup accepted to "+email+"."
	        				+"\n\nYou can now signup by visiting:"+getAppUrl(request)+"/user/signup";
	        
	        mailService.sendMail(
	        		email,
	        		subject, 
	        		body);
        	
        	model.addAttribute("message","Email '"+email+"' has been authorized");	
        } else {
        	model.addAttribute("message","There was a problem authorizing'"+email+"'");
        }
        return "auth/login";
    }
	
	@RequestMapping("/projectPageR/{projectName}")
	public String projectPageR(@PathVariable("projectName") String projectName, Model model) {
		ProjectDto projectDto = dbServices.projectGetDto(projectName);
		
		model.addAttribute("projectName",projectName);
		model.addAttribute("projectDescription",projectDto.getDescription());
		
		return "views/projectPage";
	}
	
	@RequestMapping("/decisionPageR/{id}")
	public String decisionPage(@PathVariable(value="id") Long id, Model model) {
		DecisionDtoFull decision = dbServices.decisionGetDto(id);  
		model.addAttribute("decisionId",decision.getId());
		model.addAttribute("decisionDescription",decision.getDescription());
		
		return "views/decisionPage";
	}
	
	@RequestMapping("/decisionListPageR")
	public String decisionListPage(Model model) {
		return "views/decisionListPage";
	}
	
	@RequestMapping("/projectListPageR")
	public String projectListPage(Model model) {
		return "views/projectListPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping("/cbtionNewPageR")
	public String cbtionNewPage(Model model, 
								@RequestParam(value="goalTag", required = false) String goalTag,
								@RequestParam(value="projectName", required = false) String projectName) {
		CbtionDto cbtionDto = new CbtionDto();
		if(goalTag != null) {
			cbtionDto.setGoalTag(goalTag);
		}
		if(projectName != null) {
			cbtionDto.setProjectName(projectName);
		}
		model.addAttribute("projectSelected",cbtionDto.getProjectName());
		model.addAttribute("cbtion",cbtionDto);
		return "views/cbtionNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/cbtionNewSubmit", method = RequestMethod.POST)
	public String cbtionNewSubmit(@Valid @ModelAttribute("cbtion") CbtionDto cbtionDto, BindingResult result, Model model) throws IOException {
		if(result.hasErrors()) {
			model.addAttribute("projectSelected",cbtionDto.getProjectName());
			return "views/cbtionNewPage";
		} else {
			if(dbServices.goalExist(cbtionDto.getGoalTag(),cbtionDto.getProjectName(), GoalState.ACCEPTED)) {
				/* creator is the logged user */
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				cbtionDto.setCreatorUsername(auth.getName()); 
				Long cbtionId = dbServices.cbtionCreate(cbtionDto);
				return "redirect:/views/cbtionPageR/"+cbtionId;
			} else {
				model.addAttribute("projectSelected",cbtionDto.getProjectName());
				result.rejectValue("goalTag", "goal.goalTag", "'"+cbtionDto.getGoalTag()+"' goal tag wast not found or has not been accepted yet");
				return "views/cbtionNewPage";
			}
		}
	}
	
	@Secured("ROLE_USER")
	@RequestMapping("/goalNewPageR")
	public String goalNewPage(Model model) {
		model.addAttribute("goal",new GoalDto());
		return "views/goalNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/goalNewSubmit", method = RequestMethod.POST)
	public String goalNewSubmit(@Valid @ModelAttribute("goal") GoalDto goalDto, BindingResult result, Model model) throws IOException {
		if(result.hasErrors()) {
			model.addAttribute("projectSelected",goalDto.getProjectName());
			return "views/goalNewPage";
		} else {
			/* check goal-tag is new in that project */
			if(!dbServices.goalExist(goalDto.getGoalTag(),goalDto.getProjectName())) {
				/* if parent goal is provided, check that it exist*/
				if(goalDto.getParentGoalTag() != null) {
					if(goalDto.getParentGoalTag().length() > 0) {
						if(!dbServices.goalExist(goalDto.getParentGoalTag(),goalDto.getProjectName(),GoalState.ACCEPTED)) {
							model.addAttribute("projectSelected",goalDto.getProjectName());
							result.rejectValue("parentGoalTag", "goal.parentGoalTag", "'"+goalDto.getParentGoalTag()+"' was not found, or is not yet accepted");
							return "views/goalNewPage";
						}
					}
				}
				
				/* creator is the logged user */
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				goalDto.setCreatorUsername(auth.getName()); 
				dbServices.goalCreate(goalDto);
				
				return "redirect:/views/goalPageR?projectName="+goalDto.getProjectName()+"&goalTag="+goalDto.getGoalTag();
				
			} else {
				model.addAttribute("projectSelected",goalDto.getProjectName());
				result.rejectValue("goalTag", "goal.goalTag", "'"+goalDto.getGoalTag()+"' goal tag already exist. It must be unique within the project.");
				return "views/goalNewPage";
			}
		}
	}
	
	@Secured("ROLE_USER")
	@RequestMapping("/decisionNewPageR")
	public String decisionNewPage(Model model) {
		DecisionDtoFull decisionDto = new DecisionDtoFull();
		decisionDto.setVerdictHours(36.0);
		
		model.addAttribute("decision",decisionDto);
		return "views/decisionNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/decisionNewSubmit", method = RequestMethod.POST)
	public String decisionNewSubmit(@Valid @ModelAttribute("decision") DecisionDtoCreate decisionDto, BindingResult result, Model model) throws IOException {
		if(result.hasErrors()) {
			model.addAttribute("projectSelected",decisionDto.getProjectName());
			return "views/decisionNewPage";
		} else {
			/* creator is the logged user */
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			decisionDto.setCreatorUsername(auth.getName()); 
			
			Long decisionId = dbServices.decisionCreate(decisionDto);
			return "redirect:/views/decisionPageR/"+decisionId;
		}
	}
	
	@Secured("ROLE_USER")
	@RequestMapping("/projectNewPageR")
	public String projectNewPage(Model model) {
		if(!model.containsAttribute("project")) {
			ProjectNewDto project = new ProjectNewDto();
			project.setUsernamesAndPps(new ArrayList<UsernameAndPps>());
			
			/* by default project creator is assigned 100 pps */
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			project.getUsernamesAndPps().add(new UsernameAndPps(auth.getName(),100.0));
			model.addAttribute("project",project);
		}
	
		return "views/projectNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/projectNewSubmit", params = {"addContributor"})
	public String projectNewPageAddContributor(@ModelAttribute("project") ProjectNewDto projectDto, Model model) {
		projectDto.getUsernamesAndPps().add(new UsernameAndPps("",100));
		model.addAttribute("project",projectDto);
		return "views/projectNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/projectNewSubmit", params = {"removeContributor"})
	public String projectNewPageRmContributor(@ModelAttribute("project") ProjectNewDto projectDto, Model model) {
		projectDto.getUsernamesAndPps().remove(projectDto.getUsernamesAndPps().size()-1);
		model.addAttribute("project",projectDto);
		return "views/projectNewPage";
	}
	
	@Secured("ROLE_USER")
	@RequestMapping(value="/projectNewSubmit", method = RequestMethod.POST)
	public String projectNewSubmit(@Valid @ModelAttribute("project") ProjectNewDto projectDto, BindingResult result) throws IOException {
		if(result.hasErrors()) {
			return "views/projectNewPage";
		} else {
			/* creator is the logged user */
			if(dbServices.isProjectAuthorized(projectDto.getName())) {
				if(dbServices.projectGet(projectDto.getName()) == null) {
					Authentication auth = SecurityContextHolder.getContext().getAuthentication();
					projectDto.setCreatorUsername(auth.getName()); 
					
					dbServices.projectCreate(projectDto);
					dbServices.projectCreateFirstGoal(projectDto);
					dbServices.projectStart(projectDto.getName(),projectDto.getUsernamesAndPps());
					dbServices.decisionRealmInitAllSupergoalsToProject(dbServices.projectGet(projectDto.getName()).getId());
					
					return "redirect:/views/projectPageR/"+projectDto.getName();	
				} else {
					result.rejectValue("name", "project.name", "'"+projectDto.getName()+"' already exist.");
					return "views/projectNewPage";
				}
			} else {
				result.rejectValue("name", "project.name", "'"+projectDto.getName()+"' is not authorized as a project name. Sorry, project creation requires authorization for the moment, plase contact us.");
				return "views/projectNewPage";
			}
		}
	}
	
	/* support */
	private String getAppUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
	
}
