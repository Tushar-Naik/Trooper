<#import "/spring.ftl" as spring />
<#assign url><@spring.url relativeUrl="${servletPath}/jobs.json"/></#assign>
"jobs" : { 
    "resource" : "${baseUrl}${url}",
    "registrations" : {
	<#if newjobs?? && newjobs?size!=0>
	    <#list newjobs as job>
	        "${job.name}" : {
	            <#assign job_url><@spring.url relativeUrl="${servletPath}/jobs/${job.name}.json"/></#assign>
	            "name" : "${job.name}",
	            "resource" : "${baseUrl}${job_url}",
	            "description" : "<@spring.messageText code="${job.name}.description" text="No description"/>",
	            "executionCount" : ${job.executionCount?c},
	            "CronExpression" : "<@spring.messageText code="${job.cronExpression}" text=""/>",
	            "NextFireDate" : "${job.nextFireTime}"
	        }<#if job_index != newjobs?size-1>,</#if>
	    </#list>
	</#if>
     }
}
<#if nextJob?? || previousJob??>,
  "page" : {
      "start" : ${startJob?c},
      "end" : ${endJob?c},
      "total" : ${totalJobs?c}<#if nextJob??>, 
      "next" : "${baseUrl}${url}?startJob=${nextJob?c}&pageSize=${pageSize!20}"</#if><#if previousJob??>,
      "previous" : "${baseUrl}${url}?startJob=${previousJob?c}&pageSize=${pageSize!20}"</#if>
  }
</#if>