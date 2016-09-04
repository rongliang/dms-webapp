package com.xmomen.module.wx.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xmomen.framework.mybatis.dao.MybatisDao;
import com.xmomen.module.base.entity.CdBind;
import com.xmomen.module.base.entity.CdExpressMember;
import com.xmomen.module.base.entity.CdMember;
import com.xmomen.module.order.entity.TbOrder;

/**
 * Created by Jeng on 2016/3/30.
 */
@Service
public class BindService {
	Logger logger = LoggerFactory.getLogger(BindService.class);

    @Autowired
    MybatisDao mybatisDao;

    /**
     * 绑定手机号
     * 如果之前绑定过则覆盖
     * @param openId 唯一号
     * @param phone 手机号
     * @param bindType 绑定类型 1-客户绑定 2-快递员绑定
     * @return
     */
    @Transactional
	public boolean bindAccount(String openId, String phone,String bindType){
		if("1".equals(bindType)){
			CdMember member = new CdMember();
			member.setPhoneNumber(phone);
			List<CdMember> members = mybatisDao.selectByModel(member);
			if(members == null || members.size() == 0){
				return false;
			}
		}
		else if("2".equals(bindType)){
			CdExpressMember expressMember = new CdExpressMember();
			expressMember.setPhone(phone);
			List<CdExpressMember> expressMembers = mybatisDao.selectByModel(expressMember);
			if(expressMembers == null || expressMembers.size() == 0){
				return false;
			}
		}
		CdBind bind = new CdBind();
		bind.setOpenId(openId);
		List<CdBind> binds = mybatisDao.selectByModel(bind);
		if(binds != null && binds.size() > 0){
			bind.setId(binds.get(0).getId());;
		}
		bind.setOpenId(openId);
		bind.setPhone(phone);
		mybatisDao.save(bind);
		return true;
	}
	
	/**
	 * 订单绑定快递员
	 * @return
	 */
	@Transactional
	public String bindExpressMember(String phone,String orderNo){
		//查找订单
		TbOrder order = new TbOrder();
		order.setOrderNo(orderNo);
		order = mybatisDao.selectOneByModel(order);
		//判断订单状态是否是待配送状态
		if(!"12".equals(order.getOrderStatus())){
			logger.error("订单"+orderNo+"状态不是待配送，不能扫描");
			return "订单"+orderNo+"状态不是待配送，不能扫描";
		}
		
		if(order == null){
			logger.error("订单"+orderNo+"不存在");
			return "订单<<"+orderNo+">>不存在";
		}
		//查找快递员信息
		CdExpressMember expressMember = new CdExpressMember();
		expressMember.setPhone(phone);
		List<CdExpressMember> expressMembers = mybatisDao.selectByModel(expressMember);
		if(expressMembers == null || expressMembers.size() == 0){
			logger.error("快递员不存在，手机号："+phone);
			return "快递员不存在，手机号："+phone;
		}
		expressMember = expressMembers.get(0);
		//订单中的发运快递商是否是快递员所属快递商
		if(order.getDespatchExpressId() != expressMember.getCdExpressId()){
			logger.error("不属于你的包裹，不能扫描!");
			return "不属于你的包裹，不能扫描!";
		}
		//更新订单
		order.setExpressMemberId(expressMember.getId());
		//更新订单状态
		//配送中
		order.setOrderStatus("5");
		mybatisDao.save(order);
		return "扫描成功";
	}
	
	
	/**
	 * 订单收货
	 * @return
	 */
	@Transactional
	public boolean orderShouhuo(String openId,String orderNo){
		TbOrder order = new TbOrder();
		order.setOrderNo(orderNo);
		order = mybatisDao.selectOneByModel(order);
		//收货状态
		order.setOrderStatus("6");
		mybatisDao.save(order);
		return true;
	}
}
