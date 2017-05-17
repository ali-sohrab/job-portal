package com.hireme.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hireme.dao.CompanyDao;
import com.hireme.exceptions.BusinessException;
import com.hireme.model.Company;
import com.hireme.model.JobApplication;
import com.hireme.model.JobInterest;
import com.hireme.model.JobPost;
import com.hireme.model.JobSeeker;
import com.hireme.model.User;
import com.hireme.model.id.SeekerPostId;
import com.hireme.repository.JobApplicationRepository;
import com.hireme.repository.JobInterestRepository;
import com.hireme.service.CompanyService;
import com.hireme.service.UserService;
import com.hireme.service.model.JobApplicationStatus;
import com.hireme.service.model.JobPostStatus;

@Service("companyService")
public class CompanyServiceImpl implements CompanyService {

	@Autowired
	private CompanyDao companyDao;

	@Autowired
	private UserService userService;
	
	@Autowired
	private JobApplicationRepository jobApplicationRepository;
	
	@Autowired
	private JobInterestRepository jobInterestRepository;

	@Override
	public Company getByUserId(long userId) throws BusinessException {
		return companyDao.getByUserId(userId);
	}

	@Override
	public Company createOrUpdate(long userId, Company company) throws BusinessException {
		try {
			User user = userService.getUser(userId);
			try {
				Company newCompany = getByUserId(userId);
				newCompany.setLogoURL(company.getLogoURL());
				newCompany.setDescription(company.getDescription());
				newCompany.setLocation(company.getLocation());
				newCompany.setName(company.getName());
				newCompany.setWebsite(company.getWebsite());
				return companyDao.update(newCompany);
			} catch (BusinessException be) {
				if (be.getErrorCode() == 404) {
					// Create new company
					company.setUser(user);
					return companyDao.create(company);
				} else {
					throw be;
				}
			}
		} catch (BusinessException be) {
			throw be;
		}
	}

	@Override
	public void remove(long companyId) throws BusinessException {
		companyDao.delete(companyId);
	}

	@Override
	public void removeJobPost(long userId, long jobPostId) throws BusinessException {
		Company company = getByUserId(userId);
		List<JobPost> jobPosts = company.getJobPosts();

		if (jobPosts == null) {
			throw new BusinessException(404, "No job post found with id "+ jobPostId);
		}

		for(int i=0; i < jobPosts.size(); i++) {
			if(jobPosts.get(i).getJobPostId() == jobPostId) {
				jobPosts.remove(i);
				companyDao.update(company);
				return;
			}
		}
		throw new BusinessException(404, "No job post found with id "+ jobPostId);
	}

	@Override
	public Company updateJobPost(long companyId, JobPost jobPost) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<JobPost> postJob(long userId, JobPost jobPost) throws BusinessException {
		userService.getUser(userId); // to check if user is valid
		Company company = getByUserId(userId);
		List<JobPost> jobPosts = company.getJobPosts();
		if (jobPosts == null) {
			jobPosts = new ArrayList<>();
			company.setJobPosts(jobPosts);
		}
		jobPost.setCompany(company);
		jobPost.setStatus(JobPostStatus.OPEN.name());
		jobPosts.add(jobPost);
		company = companyDao.update(company);
		return company.getJobPosts();
	}

	@Override
	public List<JobPost> updateJobPost(long userId, long jobId, JobPost jobPost) throws BusinessException {
		userService.getUser(userId); // to check if user is valid
		Company company = getByUserId(userId);
		List<JobPost> jobPosts = company.getJobPosts();

		if (jobPosts == null) {
			throw new BusinessException(404, "No job post found with id "+ jobId);
		}

		for(JobPost currentJobPost : jobPosts) {
			if(currentJobPost.getJobPostId() == jobId) {
				currentJobPost.setDescription(jobPost.getDescription());
				currentJobPost.setLocation(jobPost.getLocation());
				currentJobPost.setSalary(jobPost.getSalary());
				currentJobPost.setTitle(jobPost.getTitle());
				JobPostStatus newStatus = JobPostStatus.valueOf(jobPost.getStatus());
				JobPostStatus currentStatus = JobPostStatus.valueOf(currentJobPost.getStatus());

				if(currentStatus != newStatus) {
					currentJobPost.setStatus(newStatus.name());
					if(newStatus != JobPostStatus.OPEN) {
						//TODO update interested and applied jobs and send mail 
					}
				}	
			}
		}
		company = companyDao.update(company);
		return company.getJobPosts();
	}

	@Override
	public List<JobPost> getJobPosts(long userId) throws BusinessException {
		userService.getUser(userId); // to check if user is valid
		Company company = getByUserId(userId);
		List<JobPost> jobPosts = company.getJobPosts();
		if (jobPosts == null) {
			throw new BusinessException(404, "Nojobs posted for company with user Id " + userId);
		}
		return jobPosts;
	}

	@Override
	public void cancelJobPost(long userId, long jobPostId) throws BusinessException {
		Company company = getByUserId(userId);
		List<JobPost> jobPosts = company.getJobPosts();

		if (jobPosts == null) {
			throw new BusinessException(404, "No job post found with id "+ jobPostId);
		}

		for(int i=0; i < jobPosts.size(); i++) {
			if(jobPosts.get(i).getJobPostId() == jobPostId) {
				Set<JobApplication> appliedJobSeekers =  jobApplicationRepository.findByJobPostId(jobPostId);
				for (JobApplication jobApplication : appliedJobSeekers) {
					jobApplication.setStatus(JobApplicationStatus.CANCELLED.name());
					jobApplicationRepository.save(jobApplication);
					//TODO send mail
				}
				
				Set<JobInterest> interestedJobSeekers =  jobInterestRepository.findByJobPostId(jobPostId);
				for (JobInterest jobInterest : interestedJobSeekers) {
					jobInterestRepository.delete(jobInterest);
					//TODO send mail
				}
				
				jobPosts.get(i).setStatus(JobPostStatus.CANCELLED.toString());
				companyDao.update(company);
				return;
			}
		}
		throw new BusinessException(404, "No job post found with id "+ jobPostId);
		
	}
}
