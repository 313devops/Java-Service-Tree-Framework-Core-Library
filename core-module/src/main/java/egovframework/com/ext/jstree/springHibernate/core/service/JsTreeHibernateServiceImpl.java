package egovframework.com.ext.jstree.springHibernate.core.service;

import egovframework.com.ext.jstree.springHibernate.core.dao.JsTreeHibernateDao;
import egovframework.com.ext.jstree.springHibernate.core.vo.JsTreeHibernateSearchDTO;
import egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@Service("JsTreeHibernateService")
public class JsTreeHibernateServiceImpl implements JsTreeHibernateService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("rawtypes")
	@Resource(name = "jsTreeHibernateDao")
	private JsTreeHibernateDao jsTreeHibernateDao;

	@SuppressWarnings("unchecked")
	@Override
	public <T extends JsTreeHibernateSearchDTO> T getNode(T jsTreeHibernateDTO) throws Exception {
		logger.info("getNode");
		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		jsTreeHibernateDTO.setWhere("c_id", jsTreeHibernateDTO.getC_id());
		Object uniqueObj = jsTreeHibernateDao.getUnique(jsTreeHibernateDTO);
		return (T) uniqueObj;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends JsTreeHibernateSearchDTO> List<T> getChildNode(T jsTreeHibernateDTO) throws Exception {
 		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		jsTreeHibernateDTO.setOrder(Order.asc("c_position"));
		List<T> list = jsTreeHibernateDao.getList(jsTreeHibernateDTO);
		return list;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends JsTreeHibernateSearchDTO> List<T> getPaginatedChildNode(T jsTreeHibernateDTO) throws Exception {
		
		/** paging */
    	PaginationInfo paginationInfo = jsTreeHibernateDTO.getPaginationInfo();
	    paginationInfo.setCurrentPageNo(jsTreeHibernateDTO.getPageIndex());
	    paginationInfo.setRecordCountPerPage(jsTreeHibernateDTO.getPageUnit());
	    paginationInfo.setPageSize(jsTreeHibernateDTO.getPageSize());
	    
	    jsTreeHibernateDTO.setFirstIndex(paginationInfo.getFirstRecordIndex());
	    jsTreeHibernateDTO.setLastIndex(paginationInfo.getLastRecordIndex());
	    jsTreeHibernateDTO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
	    
		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		jsTreeHibernateDTO.setOrder(Order.asc("c_position"));
		List<T> list = jsTreeHibernateDao.getList(jsTreeHibernateDTO);
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends JsTreeHibernateSearchDTO> List<String> searchNode(T jsTreeHibernateDTO) throws Exception {
		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		jsTreeHibernateDTO.setOrder(Order.asc("c_id"));
		List<T> collectionObjects = jsTreeHibernateDao.getList(jsTreeHibernateDTO);
		List<String> returnList = new ArrayList<String>();
		for (T rowObject : collectionObjects) {
			String rowData = "#node_" + rowObject.getC_id();
			returnList.add(rowData);
		}
		return returnList;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public <T extends JsTreeHibernateSearchDTO> T addNode(T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		if (jsTreeHibernateDTO.getRef() < 0) {
			throw new RuntimeException("ref is minus");
		} else {
			T nodeByRef = (T) jsTreeHibernateDao.getUnique(jsTreeHibernateDTO.getRef());

			if ("default".equals(nodeByRef.getC_type())) {
				throw new RuntimeException("nodeByRef is default Type");
			}

			nodeByRef.setWhere("c_parentid", nodeByRef.getC_id());
			final long lastPosiotionOfNodeByRef = jsTreeHibernateDao.getCount(nodeByRef);

			jsTreeHibernateDTO.setC_position(lastPosiotionOfNodeByRef);

			long rightPointFromNodeByRef = nodeByRef.getC_right();
			rightPointFromNodeByRef = Math.max(rightPointFromNodeByRef, 1);

			long spaceOfTargetNode = 2;

			this.stretchLeftRightForMyselfFromJstree(spaceOfTargetNode, rightPointFromNodeByRef,
					jsTreeHibernateDTO.getCopy(), null, jsTreeHibernateDTO);

			long targetNodeLevel = jsTreeHibernateDTO.getRef() == 0 ? 0 : nodeByRef.getC_level() + 1;

			jsTreeHibernateDTO.setC_parentid(jsTreeHibernateDTO.getRef());
			jsTreeHibernateDTO.setC_left(rightPointFromNodeByRef);
			jsTreeHibernateDTO.setC_right(rightPointFromNodeByRef + 1);
			jsTreeHibernateDTO.setC_level(targetNodeLevel);

			long insertSeqResult = (long) jsTreeHibernateDao.insert(jsTreeHibernateDTO);
			if (insertSeqResult > 0) {
				final long SUCCESS = 1;
				jsTreeHibernateDTO.setStatus(SUCCESS);
				jsTreeHibernateDTO.setId(insertSeqResult);
			} else {
				throw new RuntimeException("????????? ?????? ?????? - ?????? ??????");
			}
		}
		return jsTreeHibernateDTO;
	}

	private <T extends JsTreeHibernateSearchDTO> void stretchLeftRightForMyselfFromJstree(long spaceOfTargetNode,
			long rightPositionFromNodeByRef, long copy, Collection<Long> c_idsByChildNodeFromNodeById,
			T jsTreeHibernateDTO) throws Exception {

		DetachedCriteria detachedLeftCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		stretchLeft(spaceOfTargetNode, rightPositionFromNodeByRef, copy, c_idsByChildNodeFromNodeById,
				detachedLeftCriteria);
		DetachedCriteria detachedRightCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		stretchRight(spaceOfTargetNode, rightPositionFromNodeByRef, copy, c_idsByChildNodeFromNodeById,
				detachedRightCriteria);
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void stretchRight(long spaceOfTargetNode,
			long rightPositionFromNodeByRef, long copy, Collection<Long> c_idsByChildNodeFromNodeById,
			DetachedCriteria detachedCriteria) {
		logger.debug("-----------------------stretchRight ??????-----------------------");
		Criterion where = Restrictions.ge("c_right", rightPositionFromNodeByRef);
		detachedCriteria.add(where);
		if (copy == 0) {
			if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
				detachedCriteria.add(Restrictions.and(Restrictions.not(Restrictions.in("c_id",
						c_idsByChildNodeFromNodeById))));
			}
		}
		detachedCriteria.addOrder(Order.asc("c_id"));
		List<T> updateTargetList = jsTreeHibernateDao.getListWithoutPaging(detachedCriteria);

		for (T perJsTreeHibernateDTO : updateTargetList) {
			perJsTreeHibernateDTO.setC_right(perJsTreeHibernateDTO.getC_right() + spaceOfTargetNode);
			try {
				jsTreeHibernateDao.update(perJsTreeHibernateDTO);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void stretchLeft(long spaceOfTargetNode,
			long rightPositionFromNodeByRef, long copy, Collection<Long> c_idsByChildNodeFromNodeById,
			DetachedCriteria detachedCriteria) {

		logger.debug("-----------------------stretchLeft ??????-----------------------");
		Criterion where = Restrictions.ge("c_left", rightPositionFromNodeByRef);
		detachedCriteria.add(where);
		if (copy == 0) {
			if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
				detachedCriteria.add(Restrictions.and(Restrictions.not(Restrictions.in("c_id",
						c_idsByChildNodeFromNodeById))));
			}
		}
		detachedCriteria.addOrder(Order.asc("c_id"));
		List<T> updateTargetList = jsTreeHibernateDao.getListWithoutPaging(detachedCriteria);

		for (T perJsTreeHibernateDTO : updateTargetList) {
			perJsTreeHibernateDTO.setC_left(perJsTreeHibernateDTO.getC_left() + spaceOfTargetNode);
			try {
				jsTreeHibernateDao.update(perJsTreeHibernateDTO);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * ??????????????? ????????? ??????????????? ????????? ????????? ?????????????????? ????????? ??????????????? ????????? ????????????.
	 * ??????????????? ?????? ?????? ????????? ???????????? ?????? ????????????
	 *
	 * @param jsTreeHibernateDTO
	 * @return T extends JsTreeHibernateSearchDTO
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> T newInstance(T jsTreeHibernateDTO) throws Exception {
		Class<T> target = (Class<T>) Class.forName(jsTreeHibernateDTO.getClass().getCanonicalName());
		return target.newInstance();
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public <T extends JsTreeHibernateSearchDTO> int removeNode(T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		Criterion whereGetNode = Restrictions.eq("c_id", jsTreeHibernateDTO.getC_id());
		T removeNode = (T) jsTreeHibernateDao.getUnique(whereGetNode);

		long spaceOfTargetNode = removeNode.getC_right() - removeNode.getC_left() + 1;

		removeNode.setSpaceOfTargetNode(spaceOfTargetNode);

		DetachedCriteria detachedDeleteCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		Criterion where = Restrictions.ge("c_left", removeNode.getC_left());
		detachedDeleteCriteria.add(where);
		detachedDeleteCriteria.add(Restrictions.and(Restrictions.le("c_right", removeNode.getC_right())));
		detachedDeleteCriteria.addOrder(Order.asc("c_id"));
		try {
			List<T> deleteTargetList = jsTreeHibernateDao.getListWithoutPaging(detachedDeleteCriteria);
			for (T deleteJsTreeHibernateDTO : deleteTargetList) {
				jsTreeHibernateDao.delete(deleteJsTreeHibernateDTO);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		DetachedCriteria detachedRemovedAfterLeftFixCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		Criterion whereRemovedAfterLeftFix = Restrictions.gt("c_left", removeNode.getC_right());
		detachedRemovedAfterLeftFixCriteria.add(whereRemovedAfterLeftFix);
		detachedRemovedAfterLeftFixCriteria.addOrder(Order.asc("c_id"));
		List<T> updateRemovedAfterLeftFixtList = jsTreeHibernateDao
				.getListWithoutPaging(detachedRemovedAfterLeftFixCriteria);
		for (T perLeftFixJsTreeHibernateDTO : updateRemovedAfterLeftFixtList) {
			perLeftFixJsTreeHibernateDTO.setC_left(perLeftFixJsTreeHibernateDTO.getC_left() - spaceOfTargetNode);
			jsTreeHibernateDao.update(perLeftFixJsTreeHibernateDTO);
		}

		DetachedCriteria detachedRemovedAfterRightFixCriteria = DetachedCriteria
				.forClass(jsTreeHibernateDTO.getClass());
		Criterion whereRemovedAfterRightFix = Restrictions.gt("c_right", removeNode.getC_left());
		detachedRemovedAfterRightFixCriteria.add(whereRemovedAfterRightFix);
		detachedRemovedAfterRightFixCriteria.addOrder(Order.asc("c_id"));
		List<T> updateRemovedAfterRightFixtList = jsTreeHibernateDao
				.getListWithoutPaging(detachedRemovedAfterRightFixCriteria);
		for (T perRightFixJsTreeHibernateDTO : updateRemovedAfterRightFixtList) {
			perRightFixJsTreeHibernateDTO.setC_right(perRightFixJsTreeHibernateDTO.getC_right() - spaceOfTargetNode);
			jsTreeHibernateDao.update(perRightFixJsTreeHibernateDTO);
		}

		DetachedCriteria detachedRemovedAfterPositionFixCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO
				.getClass());
		Criterion whereRemovedAfterPositionFix = Restrictions.eq("c_parentid", removeNode.getC_parentid());
		detachedRemovedAfterPositionFixCriteria.add(whereRemovedAfterPositionFix);
		detachedRemovedAfterPositionFixCriteria.add(Restrictions.and(Restrictions.gt("c_position",
				removeNode.getC_position())));
		detachedRemovedAfterPositionFixCriteria.addOrder(Order.asc("c_id"));
		List<T> updateRemovedAfterPositionFixtList = jsTreeHibernateDao
				.getListWithoutPaging(detachedRemovedAfterPositionFixCriteria);
		for (T perPositionFixJsTreeHibernateDTO : updateRemovedAfterPositionFixtList) {
			perPositionFixJsTreeHibernateDTO.setC_position(perPositionFixJsTreeHibernateDTO.getC_position() - 1);
			jsTreeHibernateDao.update(perPositionFixJsTreeHibernateDTO);
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public <T extends JsTreeHibernateSearchDTO> int alterNode(T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		T alterTargetNode = (T) jsTreeHibernateDao.getUnique(jsTreeHibernateDTO.getC_id());
		alterTargetNode.setC_title(jsTreeHibernateDTO.getC_title());
		alterTargetNode.setFieldFromNewInstance(jsTreeHibernateDTO);
		jsTreeHibernateDao.update(alterTargetNode);
		return 1;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public <T extends JsTreeHibernateSearchDTO> int alterNodeType(T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		T nodeById = (T) jsTreeHibernateDao.getUnique(jsTreeHibernateDTO.getC_id());

		if (nodeById.getC_type().equals(jsTreeHibernateDTO.getC_type())) {
			return 1;
		} else if ("default".equals(jsTreeHibernateDTO.getC_type())) {
			nodeById.setWhere("c_parentid", nodeById.getC_id());
			List<T> childNodesFromNodeById = jsTreeHibernateDao.getList(nodeById);
			if (childNodesFromNodeById.size() != 0) {
				throw new RuntimeException("????????? ????????? ????????? ???????????? ???????????? ???");
			} else {
				nodeById.setC_type(jsTreeHibernateDTO.getC_type());
				jsTreeHibernateDao.update(nodeById);
			}
		} else if ("folder".equals(jsTreeHibernateDTO.getC_type())) {
			nodeById.setC_type(jsTreeHibernateDTO.getC_type());
			jsTreeHibernateDao.update(nodeById);
			return 1;
		}
		return 1;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public <T extends JsTreeHibernateSearchDTO> T moveNode(T jsTreeHibernateDTO, HttpServletRequest request)
			throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());

		logger.debug("***********************MoveNode***********************");
		logger.debug("-----------------------getNode ??????-----------------------");

		T nodeById = getNode(jsTreeHibernateDTO);
		if (nodeById == null) {
			throw new RuntimeException("nodeById is null");
		}
		Long nodeByIdLeft = nodeById.getC_left();

		logger.debug("-----------------------getChildNodeByLeftRight ??????-----------------------");
		DetachedCriteria getChildNodeByLeftRightCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		Criterion whereChildNodeByLeftRight = Restrictions.ge("c_left", nodeById.getC_left());
		getChildNodeByLeftRightCriteria.add(whereChildNodeByLeftRight);
		getChildNodeByLeftRightCriteria.add(Restrictions.and(Restrictions.le("c_right", nodeById.getC_right())));
		getChildNodeByLeftRightCriteria.addOrder(Order.asc("c_left"));
		List<T> childNodesFromNodeById = jsTreeHibernateDao.getListWithoutPaging(getChildNodeByLeftRightCriteria);

		logger.debug("-----------------------nodeByRef ??????-----------------------");
		T nodeByRef = (T) jsTreeHibernateDao.getUnique(jsTreeHibernateDTO.getRef());
		long rightPointFromNodeByRef = nodeByRef.getC_right();

		logger.debug("-----------------------childNodesFromNodeByRef ??????-----------------------");
		DetachedCriteria getNodeByRefCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		Criterion whereNodeByRef = Restrictions.eq("c_parentid", nodeByRef.getC_id());
		getNodeByRefCriteria.add(whereNodeByRef);
		List<T> childNodesFromNodeByRef = (List<T>) jsTreeHibernateDao.getListWithoutPaging(getNodeByRefCriteria);

		T t_ComprehensiveTree = newInstance(jsTreeHibernateDTO);

		long spaceOfTargetNode = 2;
		Collection<Long> c_idsByChildNodeFromNodeById = null;

		logger.debug("-----------------------c_idsByChildNodeFromNodeById ??????-----------------------");
		c_idsByChildNodeFromNodeById = CollectionUtils.collect(childNodesFromNodeById, new Transformer<T, Long>() {
			@Override
			public Long transform(T childNodePerNodeById) {
				return childNodePerNodeById.getC_id();
			}
		});

		if (c_idsByChildNodeFromNodeById.contains(jsTreeHibernateDTO.getRef())) {
			throw new RuntimeException("myself contains already refTargetNode");
		}

		spaceOfTargetNode = nodeById.getC_right() - nodeById.getC_left() + 1;

		if (!jsTreeHibernateDTO.isCopied()) {
			logger.debug("-----------------------cutMyself ??????-----------------------");
			this.cutMyself(nodeById, spaceOfTargetNode, c_idsByChildNodeFromNodeById);
		}

		logger.debug("-----------------------calculatePostion ??????-----------------------");
		this.calculatePostion(jsTreeHibernateDTO, nodeById, childNodesFromNodeByRef, request);

		if (rightPointFromNodeByRef < 1) {
			rightPointFromNodeByRef = 1;
		}

		if (!jsTreeHibernateDTO.isCopied()) {
			logger.debug("-----------------------stretchPositionForMyselfFromJstree ??????-----------------------");
			this.stretchPositionForMyselfFromJstree(c_idsByChildNodeFromNodeById, jsTreeHibernateDTO);

			int selfPosition = (nodeById.getC_parentid() == jsTreeHibernateDTO.getRef() && jsTreeHibernateDTO
					.getC_position() > nodeById.getC_position()) ? 1 : 0;

			for (T child : childNodesFromNodeByRef) {
				if (child.getC_position() - selfPosition == jsTreeHibernateDTO.getC_position()) {
					rightPointFromNodeByRef = child.getC_left();
					break;
				}
			}

			if (nodeById.getC_left() < rightPointFromNodeByRef) {
				rightPointFromNodeByRef -= spaceOfTargetNode;
			}
		}

		logger.debug("-----------------------stretchLeftRightForMyselfFromJstree ??????-----------------------");
		this.stretchLeftRightForMyselfFromJstree(spaceOfTargetNode, rightPointFromNodeByRef,
				jsTreeHibernateDTO.getCopy(), c_idsByChildNodeFromNodeById, jsTreeHibernateDTO);

		if (logger.isDebugEnabled()) {
			logger.debug(">>>>>>>>>>>>>>>>>>>>" + rightPointFromNodeByRef);
		}

		long targetNodeLevel = nodeById.getC_level() - (nodeByRef.getC_level() + 1);
		long comparePoint = nodeByIdLeft - rightPointFromNodeByRef;

		if (logger.isDebugEnabled()) {
			logger.debug(">>>>>>>>>>>>>>>>>>>>" + comparePoint);
		}

		if (jsTreeHibernateDTO.isCopied()) {
			logger.debug("-----------------------pasteMyselfFromJstree ??????-----------------------");
			long insertSeqResult = this
					.pasteMyselfFromJstree(jsTreeHibernateDTO.getRef(), comparePoint, spaceOfTargetNode,
							targetNodeLevel, c_idsByChildNodeFromNodeById, rightPointFromNodeByRef, nodeById);
			t_ComprehensiveTree.setId(insertSeqResult);
			logger.debug("-----------------------fixPositionParentIdOfCopyNodes-----------------------");
			this.fixPositionParentIdOfCopyNodes(insertSeqResult, jsTreeHibernateDTO.getC_position(), jsTreeHibernateDTO);
		} else {
			logger.debug("-----------------------enterMyselfFromJstree ??????-----------------------");
			this.enterMyselfFromJstree(jsTreeHibernateDTO.getRef(), jsTreeHibernateDTO.getC_position(),
					jsTreeHibernateDTO.getC_id(), comparePoint, targetNodeLevel, c_idsByChildNodeFromNodeById,
					jsTreeHibernateDTO);
			enterMyselfFixLeftRight(comparePoint, targetNodeLevel, c_idsByChildNodeFromNodeById, jsTreeHibernateDTO);
		}
		return t_ComprehensiveTree;
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void enterMyselfFromJstree(long ref, long c_position, long c_id,
			long idif, long ldif, Collection<Long> c_idsByChildNodeFromNodeById, T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		logger.debug("-----------------------enterMyselfFixPosition-----------------------");

		T childEnterMyselfFixPosition = (T) jsTreeHibernateDao.getUnique(jsTreeHibernateDTO.getC_id());
		childEnterMyselfFixPosition.setC_parentid(ref);
		childEnterMyselfFixPosition.setC_position(c_position);
		jsTreeHibernateDao.update(childEnterMyselfFixPosition);

	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void enterMyselfFixLeftRight(long idif, long ldif,
			Collection<Long> c_idsByChildNodeFromNodeById, T jsTreeHibernateDTO) {
		logger.debug("-----------------------enterMyselfFixLeftRight-----------------------");
		DetachedCriteria detachedEnterMyselfFixLeftRightCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO
				.getClass());
		if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
			Criterion whereEnterMyselfFixLeftRight = Restrictions.in("c_id", c_idsByChildNodeFromNodeById);
			detachedEnterMyselfFixLeftRightCriteria.add(whereEnterMyselfFixLeftRight);
			detachedEnterMyselfFixLeftRightCriteria.addOrder(Order.asc("c_id"));

			List<T> enterMyselfFixLeftRightList = jsTreeHibernateDao
					.getListWithoutPaging(detachedEnterMyselfFixLeftRightCriteria);
			for (T perEnterMyselfFixLeftRightList : enterMyselfFixLeftRightList) {
				logger.debug(perEnterMyselfFixLeftRightList.toString());
				perEnterMyselfFixLeftRightList.setC_left(perEnterMyselfFixLeftRightList.getC_left() - idif);
				perEnterMyselfFixLeftRightList.setC_right(perEnterMyselfFixLeftRightList.getC_right() - idif);
				perEnterMyselfFixLeftRightList.setC_level(perEnterMyselfFixLeftRightList.getC_level() - ldif);
				jsTreeHibernateDao.update(perEnterMyselfFixLeftRightList);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void fixPositionParentIdOfCopyNodes(long insertSeqResult,
			long position, T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());

		T node = (T) jsTreeHibernateDao.getUnique(insertSeqResult);

		logger.debug("-----------------------fixPositionParentIdOfCopyNodes ??????-----------------------");
		DetachedCriteria getChildNodeByLeftRightCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO.getClass());
		Criterion whereChildNodeByLeftRight = Restrictions.ge("c_left", node.getC_left());
		getChildNodeByLeftRightCriteria.add(whereChildNodeByLeftRight);
		getChildNodeByLeftRightCriteria.add(Restrictions.and(Restrictions.le("c_right", node.getC_right())));
		getChildNodeByLeftRightCriteria.addOrder(Order.asc("c_left"));
		List<T> children = jsTreeHibernateDao.getListWithoutPaging(getChildNodeByLeftRightCriteria);

		Map<Long, Long> parentIds = new HashMap<Long, Long>();

		for (T child : children) {
			for (long i = child.getC_left() + 1; i < child.getC_right(); i++) {
				long parentId = child.getC_id();
				parentIds.put(i, parentId);
			}

			if (child.getC_id() == insertSeqResult) {
				if (logger.isDebugEnabled()) {
					logger.debug(">>>>>>>>>>>>>>>>> ??????????????? ?????????.");
					logger.debug("C_TITLE    = " + child.getC_title());
					logger.debug("C_ID       = " + insertSeqResult);
					logger.debug("C_POSITION = " + position);
				}

				node.setC_position(position);

				jsTreeHibernateDao.update(node);
				continue;
			}

			if (logger.isDebugEnabled()) {
				logger.debug(">>>>>>>>>>>>>>>>> ???????????? ?????? ?????? ?????????");
				logger.debug("C_TITLE    = " + child.getC_title());
				logger.debug("C_ID       = " + child.getC_id());
				logger.debug("C_POSITION = " + child.getC_position());
				logger.debug("C_PARENTID = " + child.getC_parentid());
				logger.debug("?????????????????? = " + parentIds.get(child.getC_left()));
			}

			child.setFixCopyId(parentIds.get(child.getC_left()));
			child.setC_parentid(parentIds.get(child.getC_left()));
			jsTreeHibernateDao.update(child);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> long pasteMyselfFromJstree(long ref, long idif,
			long spaceOfTargetNode, long ldif, Collection<Long> c_idsByChildNodeFromNodeById,
			long rightPositionFromNodeByRef, T nodeById) throws Exception {

		jsTreeHibernateDao.setClazz(nodeById.getClass());

		T onlyPasteMyselfFromJstree = getNode(nodeById);
		
		onlyPasteMyselfFromJstree.setRef(ref);
		onlyPasteMyselfFromJstree.setIdif(idif);
		onlyPasteMyselfFromJstree.setSpaceOfTargetNode(spaceOfTargetNode);
		onlyPasteMyselfFromJstree.setLdif(ldif);
		onlyPasteMyselfFromJstree.setC_idsByChildNodeFromNodeById(c_idsByChildNodeFromNodeById);
		onlyPasteMyselfFromJstree.setRightPositionFromNodeByRef(rightPositionFromNodeByRef);
		onlyPasteMyselfFromJstree.setNodeById(nodeById);

		onlyPasteMyselfFromJstree.setIdifLeft(idif
				+ (nodeById.getC_left() >= rightPositionFromNodeByRef ? spaceOfTargetNode : 0));
		onlyPasteMyselfFromJstree.setIdifRight(idif
				+ (nodeById.getC_left() >= rightPositionFromNodeByRef ? spaceOfTargetNode : 0));

		DetachedCriteria detachedPasteMyselfFromJstreeCriteria = DetachedCriteria.forClass(nodeById
				.getClass());
		if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
			Criterion wherePasteMyselfFromJstree = Restrictions.in("c_id", c_idsByChildNodeFromNodeById);
			detachedPasteMyselfFromJstreeCriteria.add(wherePasteMyselfFromJstree);
			detachedPasteMyselfFromJstreeCriteria.addOrder(Order.desc("c_level"));

			List<T> pasteMyselfFromJstreeList = jsTreeHibernateDao
					.getListWithoutPaging(detachedPasteMyselfFromJstreeCriteria);
			for (T perPasteMyselfFromJstree : pasteMyselfFromJstreeList) {
				logger.debug("------pasteMyselfFromJstree------LOOP---" + perPasteMyselfFromJstree.getC_id());
				T addTarget = newInstance(perPasteMyselfFromJstree);
				
				addTarget.setC_parentid(onlyPasteMyselfFromJstree.getRef());
				addTarget.setC_position(perPasteMyselfFromJstree.getC_position());
				addTarget.setC_left(perPasteMyselfFromJstree.getC_left() - onlyPasteMyselfFromJstree.getIdifLeft());
				addTarget.setC_right(perPasteMyselfFromJstree.getC_right() - onlyPasteMyselfFromJstree.getIdifRight());
				addTarget.setC_level(perPasteMyselfFromJstree.getC_level() - onlyPasteMyselfFromJstree.getLdif());
				addTarget.setC_title(perPasteMyselfFromJstree.getC_title());
				addTarget.setC_type(perPasteMyselfFromJstree.getC_type());
				
				addTarget.setFieldFromNewInstance(perPasteMyselfFromJstree);
				logger.debug("????????? ??????????????? ????????? ????????? ?????? ????????? ???????????? ?????? ????????? ??????");
				
				long insertSeqResult = (long) jsTreeHibernateDao.insert(addTarget);
				perPasteMyselfFromJstree.setId(insertSeqResult);
				
				if (insertSeqResult > 0) {
					return insertSeqResult;
				} else {
					throw new RuntimeException("????????? ?????? ?????? - ?????? ??????");
				}
			}
		}

		return 0;
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void stretchPositionForMyselfFromJstree(
			Collection<Long> c_idsByChildNodeFromNodeById, T jsTreeHibernateDTO) throws Exception {

		jsTreeHibernateDao.setClazz(jsTreeHibernateDTO.getClass());
		jsTreeHibernateDTO.setC_idsByChildNodeFromNodeById(c_idsByChildNodeFromNodeById);

		DetachedCriteria detachedStretchPositionForMyselfCriteria = DetachedCriteria.forClass(jsTreeHibernateDTO
				.getClass());
		Criterion whereStretchPositionForMyself = Restrictions.eq("c_parentid", jsTreeHibernateDTO.getRef());
		detachedStretchPositionForMyselfCriteria.add(whereStretchPositionForMyself);
		detachedStretchPositionForMyselfCriteria.add(Restrictions.and(Restrictions.ge("c_position",
				jsTreeHibernateDTO.getC_position())));
		if (jsTreeHibernateDTO.getCopy() == 0) {
			if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
				detachedStretchPositionForMyselfCriteria.add(Restrictions.and(Restrictions.not(Restrictions.in("c_id",
						c_idsByChildNodeFromNodeById))));
			}
		}
		detachedStretchPositionForMyselfCriteria.addOrder(Order.asc("c_id"));

		List<T> stretchPositionForMyselfList = jsTreeHibernateDao
				.getListWithoutPaging(detachedStretchPositionForMyselfCriteria);
		for (T perStretchPositionForMyself : stretchPositionForMyselfList) {
			perStretchPositionForMyself.setC_position(perStretchPositionForMyself.getC_position() + 1);
			jsTreeHibernateDao.update(perStretchPositionForMyself);
		}

	}

	private <T extends JsTreeHibernateSearchDTO> void calculatePostion(T jsTreeHibernateDTO, T nodeById,
			List<T> childNodesFromNodeByRef, HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession();

		final boolean isMoveNodeInMyParent = (jsTreeHibernateDTO.getRef() == nodeById.getC_parentid());
		final boolean isMultiCounterZero = (jsTreeHibernateDTO.getMultiCounter() == 0);
		final boolean isBeyondTheCurrentToMoveNodes = (jsTreeHibernateDTO.getC_position() > nodeById.getC_position());

		if (isMoveNodeInMyParent) {
			if (logger.isDebugEnabled()) {
				logger.debug(">>>>>>>>>>>>>>>????????? ????????? ??? ??????????????? ????????????");
			}

			if (isMultiCounterZero) {
				if (isBeyondTheCurrentToMoveNodes) {
					if (logger.isDebugEnabled()) {
						logger.debug(">>>>>>>>>>>>>>>?????? ??? ????????? ???????????? ?????????");
						logger.debug("?????????=" + nodeById.getC_title());
						logger.debug("????????? ?????? ?????????=" + nodeById.getC_position());
						logger.debug("????????? ???????????? ?????????=" + jsTreeHibernateDTO.getC_position());
						logger.debug("????????? ???????????? ???????????????=" + jsTreeHibernateDTO.getMultiCounter());
					}

					final boolean isFolderToMoveNodes = (jsTreeHibernateDTO.getC_position() > childNodesFromNodeByRef
							.size());

					if (isFolderToMoveNodes) {
						if (logger.isDebugEnabled()) {
							logger.debug("?????? ????????? ????????? ???????????? ????????? ????????? ?????? ?????? =" + jsTreeHibernateDTO.getC_position());
						}
						long childNodesFromNodeByRefCnt = childNodesFromNodeByRef.size();
						jsTreeHibernateDTO.setC_position(childNodesFromNodeByRefCnt);
					} else {
						jsTreeHibernateDTO.setC_position(jsTreeHibernateDTO.getC_position() - 1);
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("????????? ?????? ?????????=" + jsTreeHibernateDTO.getC_position());
				}
				session.setAttribute("settedPosition", jsTreeHibernateDTO.getC_position());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug(">>>>>>>>>>>>>>>?????? ???????????? 0 ??? ?????????");
					logger.debug("?????????=" + nodeById.getC_title());
					logger.debug("????????? ?????? ?????????=" + nodeById.getC_position());
					logger.debug("????????? ???????????? ?????????=" + jsTreeHibernateDTO.getC_position());
					logger.debug("????????? ???????????? ???????????????=" + jsTreeHibernateDTO.getMultiCounter());
					logger.debug("0??? ????????? ?????????=" + session.getAttribute("settedPosition"));
				}

				long increasePosition = 0;

				final boolean isMultiNodeOfPositionsAtZeroThanBehind = ((Integer) session
						.getAttribute("settedPosition") < nodeById.getC_position());

				if (isMultiNodeOfPositionsAtZeroThanBehind) {
					if (logger.isDebugEnabled()) {
						logger.debug(">>>>>>>>>>>>>>>?????? ????????? ????????? 0??? ???????????? ?????????");
					}

					increasePosition = (Integer) session.getAttribute("settedPosition") + 1;
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug(">>>>>>>>>>>>>>>?????? ????????? ????????? 0??? ???????????? ?????????");
					}

					if (jsTreeHibernateDTO.isCopied()) {
						increasePosition = (Integer) session.getAttribute("settedPosition") + 1;
					} else {
						increasePosition = (Integer) session.getAttribute("settedPosition");
					}

				}
				session.setAttribute("settedPosition", increasePosition);

				jsTreeHibernateDTO.setC_position(increasePosition);

				final boolean isSamePosition = (nodeById.getC_position() == jsTreeHibernateDTO.getC_position());

				if (isSamePosition) {
					if (logger.isDebugEnabled()) {
						logger.debug(">>>>>>>>>>>>>>>?????? ?????? ???????????? ?????? ????????? ????????? ???????????? ????????? ??????");
					}

					session.setAttribute("settedPosition", increasePosition - 1);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("????????? ?????? ?????????=" + jsTreeHibernateDTO.getC_position());
				}
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug(">>>>>>>>>>>>>>>????????? ????????? ??? ??????????????? ????????????");
			}

			if (isMultiCounterZero) {
				if (logger.isDebugEnabled()) {
					logger.debug(">>>>>>>>>>>>>>>?????? ???????????? 0 ??????");
					logger.debug("?????????=" + nodeById.getC_title());
					logger.debug("????????? ?????? ?????????=" + nodeById.getC_position());
					logger.debug("????????? ???????????? ?????????=" + jsTreeHibernateDTO.getC_position());
					logger.debug("????????? ???????????? ???????????????=" + jsTreeHibernateDTO.getMultiCounter());
					logger.debug("????????? ?????? ?????????=" + jsTreeHibernateDTO.getC_position());
				}

				session.setAttribute("settedPosition", jsTreeHibernateDTO.getC_position());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug(">>>>>>>>>>>>>>>?????? ???????????? 0 ??? ?????????");
					logger.debug("?????????=" + nodeById.getC_title());
					logger.debug("????????? ?????? ?????????=" + nodeById.getC_position());
					logger.debug("????????? ???????????? ?????????=" + jsTreeHibernateDTO.getC_position());
					logger.debug("????????? ???????????? ???????????????=" + jsTreeHibernateDTO.getMultiCounter());
				}

				long increasePosition = 0;
				increasePosition = NumberUtils.toLong(session.getAttribute("settedPosition").toString()) + 1;
				jsTreeHibernateDTO.setC_position(increasePosition);
				session.setAttribute("settedPosition", increasePosition);

				if (logger.isDebugEnabled()) {
					logger.debug("????????? ?????? ?????????=" + jsTreeHibernateDTO.getC_position());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends JsTreeHibernateSearchDTO> void cutMyself(T nodeById, long spaceOfTargetNode,
			Collection<Long> c_idsByChildNodeFromNodeById) throws Exception {

		jsTreeHibernateDao.setClazz(nodeById.getClass());
		nodeById.setSpaceOfTargetNode(spaceOfTargetNode);
		nodeById.setC_idsByChildNodeFromNodeById(c_idsByChildNodeFromNodeById);

		logger.debug("***********************CutMyself***********************");
		logger.debug("-----------------------cutMyselfPositionFix-----------------------");
		DetachedCriteria cutMyselfPositionFixCriteria = DetachedCriteria.forClass(nodeById.getClass());
		Criterion whereCutMyselfPositionFix = Restrictions.eq("c_parentid", nodeById.getC_parentid());
		cutMyselfPositionFixCriteria.add(whereCutMyselfPositionFix);
		cutMyselfPositionFixCriteria.add(Restrictions.and(Restrictions.gt("c_position", nodeById.getC_position())));
		cutMyselfPositionFixCriteria.addOrder(Order.asc("c_id"));
		List<T> childCutMyselfPositionFix = jsTreeHibernateDao.getListWithoutPaging(cutMyselfPositionFixCriteria);
		for (T perNodeById : childCutMyselfPositionFix) {
			perNodeById.setC_position(perNodeById.getC_position() - 1);
			jsTreeHibernateDao.update(perNodeById);
		}

		logger.debug("-----------------------cutMyselfLeftFix-----------------------");
		DetachedCriteria cutMyselfLeftFixCriteria = DetachedCriteria.forClass(nodeById.getClass());
		Criterion whereCutMyselfLeftFix = Restrictions.gt("c_left", nodeById.getC_right());
		cutMyselfLeftFixCriteria.add(whereCutMyselfLeftFix);
		cutMyselfLeftFixCriteria.addOrder(Order.asc("c_id"));
		List<T> childCutMyselfLeftFix = jsTreeHibernateDao.getListWithoutPaging(cutMyselfLeftFixCriteria);
		for (T perCutMyselfLeftFix : childCutMyselfLeftFix) {
			perCutMyselfLeftFix.setC_left(perCutMyselfLeftFix.getC_left() - spaceOfTargetNode);
			jsTreeHibernateDao.update(perCutMyselfLeftFix);
		}

		logger.debug("-----------------------cutMyselfRightFix-----------------------");
		DetachedCriteria cutMyselfRightFixCriteria = DetachedCriteria.forClass(nodeById.getClass());
		Criterion whereCutMyselfRightFix = Restrictions.gt("c_right", nodeById.getC_left());
		cutMyselfRightFixCriteria.add(whereCutMyselfRightFix);
		if (c_idsByChildNodeFromNodeById != null && c_idsByChildNodeFromNodeById.size() > 0) {
			cutMyselfRightFixCriteria.add(Restrictions.and(Restrictions.not(Restrictions.in("c_id",
					c_idsByChildNodeFromNodeById))));
		}
		cutMyselfRightFixCriteria.addOrder(Order.asc("c_id"));
		List<T> childCutMyselfRightFix = jsTreeHibernateDao.getListWithoutPaging(cutMyselfRightFixCriteria);
		for (T perCutMyselfRightFix : childCutMyselfRightFix) {
			perCutMyselfRightFix.setC_right(perCutMyselfRightFix.getC_right() - spaceOfTargetNode);
			jsTreeHibernateDao.update(perCutMyselfRightFix);
		}

	}

}
