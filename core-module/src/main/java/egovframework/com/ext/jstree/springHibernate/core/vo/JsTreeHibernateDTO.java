package egovframework.com.ext.jstree.springHibernate.core.vo;

import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Entity
@Table(name = "T_COMPREHENSIVETREE_HIBER")
@SelectBeforeUpdate(value=true)
@DynamicInsert(value=true)
@DynamicUpdate(value=true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SequenceGenerator(name = "JsTreeSequence", sequenceName = "S_COMPREHENSIVETREE_HIBER", allocationSize = 1)
public class JsTreeHibernateDTO extends JsTreeHibernateSearchDTO implements Serializable {

	private static final long serialVersionUID = -6859122566734590165L;

	public JsTreeHibernateDTO() {
		super();
	}

	public JsTreeHibernateDTO(Boolean copyBooleanValue) {
		super();
		this.copyBooleanValue = copyBooleanValue;
	}

	/*
	 * Extend Bean Field
	 */
	private Boolean copyBooleanValue;

	@Transient
	public Boolean getCopyBooleanValue() {
		copyBooleanValue = false;
		if (this.getCopy() == 0) {
			copyBooleanValue = false;
		} else {
			copyBooleanValue = true;
		}
		return copyBooleanValue;
	}

	public void setCopyBooleanValue(Boolean copyBooleanValue) {
		this.copyBooleanValue = copyBooleanValue;
	}

	@Override
	public <T extends JsTreeHibernateSearchDTO> void setFieldFromNewInstance(T paramInstance) {
		
		if( paramInstance instanceof JsTreeHibernateDTO){
			if(paramInstance.isCopied()) {
				this.setC_title("copy_" + this.getC_title());
			}
		}
	}
	
}
