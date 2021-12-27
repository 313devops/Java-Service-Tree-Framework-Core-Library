package egovframework.com.ext.jstree.strutsiBatis.core.dao;

import egovframework.com.ext.jstree.strutsiBatis.core.dto.P_ComprehensiveTree;
import egovframework.com.ext.jstree.strutsiBatis.core.vo.T_ComprehensiveTree;

public interface I_DB_AlterNodeType extends
		I_GenericDao<T_ComprehensiveTree, P_ComprehensiveTree> {

	public int alterNodeType(P_ComprehensiveTree p_ComprehensiveTree, String determineDBSetting);

}
