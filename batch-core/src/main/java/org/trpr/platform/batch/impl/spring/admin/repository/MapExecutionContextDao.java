/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trpr.platform.batch.impl.spring.admin.repository;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.dao.XStreamExecutionContextStringSerializer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.SerializationUtils;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

/**
 * Trooper Implementation of {@link org.springframework.batch.core.repository.dao.MapExecutionContextDao}.
 * Added an ability to remove execution contexts
 * 
 * --- version 2.0 changelog ----
 * Reverted the copy() implementation to use Spring batch SerializationUtils. Also avoiding copy in read
 * calls. 
 * 
 * @author devashishshankar
 * @author Regunath B
 * @version 1.0, 6th March, 2013
 * @version 2.0, 9th July, 2014 
 */

@SuppressWarnings("serial")
public class MapExecutionContextDao implements ExecutionContextDao {

	private final ConcurrentMap<ContextKey, ExecutionContext> contexts = TransactionAwareProxyFactory
			.createAppendOnlyTransactionalMap();

	private ExecutionContextStringSerializer serializer;
	
	public MapExecutionContextDao() throws Exception {
		serializer = new XStreamExecutionContextStringSerializer();
		((XStreamExecutionContextStringSerializer) serializer).afterPropertiesSet();
	}

	private static final class ContextKey implements Comparable<ContextKey>, Serializable {

		private static enum Type { STEP, JOB; }

		private final Type type;
		private final long id;

		private ContextKey(Type type, long id) {
			if(type == null) {
				throw new IllegalStateException("Need a non-null type for a context");
			}
			this.type = type;
			this.id = id;
		}

		@Override
		public int compareTo(ContextKey them) {
			if(them == null) {
				return 1;
			}
			final int idCompare = new Long(this.id).compareTo(new Long(them.id)); // JDK6 Make this Long.compare(x,y)
			if(idCompare != 0) {
				return idCompare;
			}
			final int typeCompare = this.type.compareTo(them.type);
			if(typeCompare != 0) {
				return typeCompare;
			}
			return 0;
		}

		@Override
		public boolean equals(Object them) {
			if(them == null) {
				return false;
			}
			if(them instanceof ContextKey) {
				return this.equals((ContextKey)them);
			}
			return false;
		}

		public boolean equals(ContextKey them) {
			if(them == null) {
				return false;
			}
			return this.id == them.id && this.type.equals(them.type);
		}

		@Override
		public int hashCode() {
			int value = (int)(id^(id>>>32));
			switch(type) {
			case STEP: return value;
			case JOB: return ~value;
			default: throw new IllegalStateException("Unknown type encountered in switch: " + type);
			}
		}

		public static ContextKey step(long id) { return new ContextKey(Type.STEP, id); }

		public static ContextKey job(long id) { return new ContextKey(Type.JOB, id); }
	}

	public void clear() {
		contexts.clear();
	}

	private ExecutionContext copy(ExecutionContext original) {
		return (ExecutionContext) SerializationUtils.deserialize(SerializationUtils.serialize(original));
	}

	@Override
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		//return copy(contexts.get(ContextKey.step(stepExecution.getId())));
		// returning the reference to the stored ExecutionContext i.e. avoiding the expensive copy operation 
		return contexts.get(ContextKey.step(stepExecution.getId()));
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		if (executionContext != null) {
			contexts.put(ContextKey.step(stepExecution.getId()), copy(executionContext));
		}
	}

	@Override
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		//return copy(contexts.get(ContextKey.job(jobExecution.getId())));
		// returning the reference to the stored ExecutionContext i.e. avoiding the expensive copy operation 		
		return contexts.get(ContextKey.job(jobExecution.getId()));
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		if (executionContext != null) {
			contexts.put(ContextKey.job(jobExecution.getId()), copy(executionContext));
		}
	}

	@Override
	public void saveExecutionContext(JobExecution jobExecution) {
		updateExecutionContext(jobExecution);
	}

	@Override
	public void saveExecutionContext(StepExecution stepExecution) {
		updateExecutionContext(stepExecution);
	}
	
	/**
	 * Removes all the executionContexts for given stepExecution
	 * @param jobExecution
	 */
	public void removeExecutionContext(StepExecution stepExecution) {
		contexts.remove(ContextKey.step(stepExecution.getId()));		
	}
	
	/**
	 * Removes all the executionContexts for given jobExecution (Including all the stepExecutions
	 * related to the jobExecution)
	 * @param jobExecution
	 */
	public void removeExecutionContext(JobExecution jobExecution) {
		contexts.remove(ContextKey.job(jobExecution.getId()));
		//No point storing StepExecutionCOntext if jobexecutioncontext have been deleted
		for(StepExecution stepExecution : jobExecution.getStepExecutions()) {
			this.removeExecutionContext(stepExecution);
		}
	}

}