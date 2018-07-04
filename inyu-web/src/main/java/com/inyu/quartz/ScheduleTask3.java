package com.inyu.quartz;

import com.inyu.common.DateUtil;
import com.inyu.entity.QuartzProxy;
import com.inyu.entity.exception.InyuException;
import com.inyu.service.AsyncQuartzProxyService;
import com.inyu.service.utils.MyHttpUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 定时任务实现类
 *
 * @author jingu
 * Created by jinyu on 2018/4/12/012.
 */
@Configuration
@Component
@EnableScheduling
public class ScheduleTask3 implements Job, Serializable {

    private static Logger logger = LoggerFactory.getLogger(ScheduleTask1.class);
    /**
     * 队列 存储待验证的代理池
     */
    public static Queue<QuartzProxy> proxyQuene = new LinkedBlockingQueue();
    @Autowired
    private AsyncQuartzProxyService asyncQuartzProxyService;

    String baidu = "https://www.baidu.com/";

    /**
     * 验证代理
     *
     * @throws JobExecutionException
     */
    public synchronized  void executeJob() {
        logger.info("==== 定时任务实现类（代理验证）ScheduleTask ====> 开启!" + DateUtil.getStringToday());
        if (!CollectionUtils.isEmpty(proxyQuene)) {
            logger.info("==== 定时任务-代理验证===> 进行中，队列剩余：" + proxyQuene.size() + "，当前时间：" + DateUtil.getStringToday());
            throw new InyuException("队列不为空！");
        }
        List<QuartzProxy> proxyList = asyncQuartzProxyService.getProxyList();
        proxyQuene.addAll(proxyList);
        while (proxyQuene.size()>0) {
            QuartzProxy proxy = proxyQuene.poll();
            proxy.setLastValidate(DateUtil.getSqlDateShort());
            //获取页面
            String proxyPage = MyHttpUtils.sendGet(baidu, null, proxy);
            if (!proxyPage.contains("<!--STATUS OK-->")) {
                proxy.setStatus(1);
            } else {
                proxy.setStatus(0);
            }
            asyncQuartzProxyService.updateProxy(proxy);
        }

    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("====== 定时任务实现类（代理验证）ScheduleTask ====> 开启!" + DateUtil.getStringToday());
        try {
            executeJob();
        } catch (Exception e) {
            logger.error("==== 定时任务实现类（代理验证）ScheduleTask ====>异常!", e.getMessage());
            return;
        } finally {
            logger.info("==== 定时任务实现类（代理验证）ScheduleTask ====> 结束!" + DateUtil.getStringToday());
        }
    }
}
