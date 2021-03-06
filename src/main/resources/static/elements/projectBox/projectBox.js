function ProjectBox(container_id,projectData,projectName) {
	// Parent constructor
	this.container = $(container_id);
	this.project = projectData;
	this.projectName = projectName;
	this.data = [];
};

//Inheritance
ProjectBox.prototype = {
		updateProject: function() {
			GLOBAL.serverComm.projectGet(this.project.name,this.projectReceivedCallback,this);
		} ,

		projectReceivedCallback: function(projectDto) {
			this.project = projectDto;
			this.draw();
		},

		draw: function() {
			this.container.load("/elements/projectBox/projectBox.html",this.projectBoxLoaded.bind(this));
		},

		projectBoxLoaded: function() {
			$("#projectname_div",this.container).html(getProjectLink(this.project.name));
			$("#description_div",this.container).html(markdown.toHTML(this.project.description));

			if(isUserLogged()) {
				$("#projectfollow_div",this.container).show();

				if(this.project.isStarred) {
					$("#starred-img",this.container).attr("src","/images/starred-yes.png");	
				}

				if(this.project.isWatched) {
					$("#watched-img",this.container).attr("src","/images/watched-yes.png");
				}

				$("#starred-img",this.container).click(this.starredClicked.bind(this));
				$("#watched-img",this.container).click(this.watchedClicked.bind(this));
			}
		},

		starredClicked: function () {
			if(!this.project.isStarred) GLOBAL.serverComm.projectStar(this.project.id,this.updateProject, this);
			else GLOBAL.serverComm.projectUnStar(this.project.id,this.updateProject, this);
		},

		watchedClicked: function () {
			if(!this.project.isWatched) GLOBAL.serverComm.projectWatch(this.project.id,this.updateProject, this);
			else GLOBAL.serverComm.projectUnWatch(this.project.id,this.updateProject, this);
		},
}
