package com.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Calendar;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.utils.PageUtils;
import com.utils.Query;
import com.dao.ZhichuDao;
import com.dao.ShouruDao;
import com.entity.ZhichuEntity;
import com.entity.ShouruEntity;


import com.dao.XiaofeiyusuanDao;
import com.entity.XiaofeiyusuanEntity;
import com.service.XiaofeiyusuanService;
import com.entity.vo.XiaofeiyusuanVO;
import com.entity.view.XiaofeiyusuanView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service("xiaofeiyusuanService")
public class XiaofeiyusuanServiceImpl extends ServiceImpl<XiaofeiyusuanDao, XiaofeiyusuanEntity> implements XiaofeiyusuanService {
	
	@Autowired
	private ZhichuDao zhichuDao;
	
	@Autowired
	private ShouruDao shouruDao;
	
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Page<XiaofeiyusuanEntity> page = this.selectPage(
                new Query<XiaofeiyusuanEntity>(params).getPage(),
                new EntityWrapper<XiaofeiyusuanEntity>()
        );
        return new PageUtils(page);
    }
    
    @Override
	public PageUtils queryPage(Map<String, Object> params, Wrapper<XiaofeiyusuanEntity> wrapper) {
		  Page<XiaofeiyusuanView> page =new Query<XiaofeiyusuanView>(params).getPage();
	        page.setRecords(baseMapper.selectListView(page,wrapper));
	    	PageUtils pageUtil = new PageUtils(page);
	    	return pageUtil;
 	}
    
    @Override
	public List<XiaofeiyusuanVO> selectListVO(Wrapper<XiaofeiyusuanEntity> wrapper) {
 		return baseMapper.selectListVO(wrapper);
	}
	
	@Override
	public XiaofeiyusuanVO selectVO(Wrapper<XiaofeiyusuanEntity> wrapper) {
 		return baseMapper.selectVO(wrapper);
	}
	
	@Override
	public List<XiaofeiyusuanView> selectListView(Wrapper<XiaofeiyusuanEntity> wrapper) {
		return baseMapper.selectListView(wrapper);
	}

	@Override
	public XiaofeiyusuanView selectView(Wrapper<XiaofeiyusuanEntity> wrapper) {
		return baseMapper.selectView(wrapper);
	}

	@Override
	@Transactional
	public void checkBudgetOverdraft(String zhanghao) {
		if (zhanghao == null || "".equals(zhanghao)) {
			return;
		}
		
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		
		List<ZhichuEntity> zhichuList = zhichuDao.selectList(
			new EntityWrapper<ZhichuEntity>()
				.eq("zhanghao", zhanghao)
				.and("YEAR(shijian) = {0}", year)
				.and("MONTH(shijian) = {0}", month)
		);
		
		List<ShouruEntity> shouruList = shouruDao.selectList(
			new EntityWrapper<ShouruEntity>()
				.eq("zhanghao", zhanghao)
				.and("YEAR(shijian) = {0}", year)
				.and("MONTH(shijian) = {0}", month)
		);
		
		float totalZhichu = 0;
		for (ZhichuEntity z : zhichuList) {
			if (z.getJine() != null) {
				totalZhichu += z.getJine();
			}
		}
		
		float totalShouru = 0;
		for (ShouruEntity s : shouruList) {
			if (s.getJine() != null) {
				totalShouru += s.getJine();
			}
		}
		
		float dangyueleiji = totalZhichu + totalShouru;
		
		List<XiaofeiyusuanEntity> yusuanList = this.selectList(
			new EntityWrapper<XiaofeiyusuanEntity>()
				.eq("zhanghao", zhanghao)
		);
		
		if (yusuanList != null && !yusuanList.isEmpty()) {
			XiaofeiyusuanEntity yusuan = yusuanList.get(0);
			float yusuanjine = yusuan.getYusuanjine() != null ? yusuan.getYusuanjine() : 0;
			
			String shifouzhichao = "否";
			if (dangyueleiji > yusuanjine) {
				shifouzhichao = "是";
			}
			
			yusuan.setDangyueleiji(dangyueleiji);
			yusuan.setShifouzhichao(shifouzhichao);
			
			this.updateById(yusuan);
		}
	}

}
