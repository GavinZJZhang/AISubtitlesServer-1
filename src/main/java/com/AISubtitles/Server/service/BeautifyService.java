package com.AISubtitles.Server.service;

import com.AISubtitles.Server.dao.VideoDao;
import com.AISubtitles.Server.domain.Result;
import com.AISubtitles.Server.domain.Video;
import com.AISubtitles.common.CodeConsts;
import org.opencv.core.Core;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Service
public class BeautifyService {

	@Autowired
	VideoDao videoDao;

	private static String exePath = "src/main/resources/lib/ffmpeg.exe";
	private static String imageFolderPath = "src/main/resources/imgs/shot";
	private static String newImageFolderPath = "src/main/resources/imgs/shot_new";
	private static String imagePath = imageFolderPath + "/%d" + ".jpg";
	private static String newImagePath = newImageFolderPath + "/%d" + ".jpg";

	private static Imagebase64Service imagebase64Service;
	private static BeautifyPicService beautifyPicService;

	private static String audioPath = "src/main/resources/tables/audio.mp3";

	private String videosPath = "src/main/resources/videos";

	private static int threadsNums;

	public BeautifyService() {
		imagebase64Service = new Imagebase64Service();
		beautifyPicService = new BeautifyPicService();
		threadsNums = 8;
	}

	public void setThreadsNums(int threadsNums) {
		this.threadsNums = threadsNums;
	}

	public Result beautify(Integer videoId, String newVideoName, int white, int smooth, int facelift, int eye) {

		Result result = new Result();

		CountDownLatch latch = new CountDownLatch(threadsNums);

		Video oldVideo = videoDao.findById(videoId).get();

		String videoPath = oldVideo.getVideoPath();
		String newVideoPath = videosPath + "/" + newVideoName;

		System.out.println("分帧开始");
		FrameProcessService frameProcessService = new FrameProcessService();
		try {
			frameProcessService.split(exePath, videoPath, imageFolderPath, audioPath);
		} catch (Exception e) {
			result.setCode(CodeConsts.CODE_SERVER_ERROR);
			result.setData("视频分帧失败");
			return result;
		}
		System.out.println("分帧成功");

		File file = new File(imageFolderPath);
		File[] files = file.listFiles();
		int nums = files.length;
		for (int i = 0; i < threadsNums; i++) {
			FilterThread testThread = new FilterThread(latch, nums*i/threadsNums+1, nums*(i+1)/threadsNums, white, smooth, facelift, eye);
			testThread.start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {}


		deleteFolder(imageFolderPath);
		System.out.println("处理成功");
		try {
			frameProcessService.integrate(exePath, newVideoPath, newImageFolderPath, videoPath, audioPath);
			result.setCode(CodeConsts.CODE_SUCCESS);
		} catch (Exception e) {
			result.setCode(CodeConsts.CODE_SERVER_ERROR);
			result.setData("视频合帧失败");
			return result;
		}

		deleteFolder(newImageFolderPath);
		System.out.println("和帧成功");
		Video newVideo = new Video();
		newVideo.setVideoPath(newVideoPath);
		newVideo.setVideoBrowses(0);
//		newVideo.setVideoDuration(oldVideo.getVideoDuration());
		newVideo.setVideoFavors(0);
//		newVideo.setVideoFormat(oldVideo.getVideoFormat());
		newVideo.setVideoName(newVideoName);
		newVideo.setVideoShares(0);
//		newVideo.setVideoSize(oldVideo.getVideoSize());
//		newVideo.setVideoCover(oldVideo.getVideoCover());
//		newVideo.setVideoENSubtitle(oldVideo.getVideoENSubtitle());
//		newVideo.setVideoENZHSubtitle(oldVideo.getVideoENZHSubtitle());
//		newVideo.setVideoENZHSubtitleJSON(oldVideo.getVideoENZHSubtitleJSON());
//		newVideo.setVideoZHSubtitle(oldVideo.getVideoZHSubtitle());
//		videoDao.save(newVideo);
		result.setData(newVideo);

		return result;
	}

	class FilterThread extends Thread{

		private int start, end;
		private CountDownLatch latch;
		private int white;
		private int smooth;
		private int facelift;
		private int eye;

		public FilterThread(CountDownLatch latch, int start, int end,
							int white, int smooth, int facelift, int eye) {
			this.latch = latch;
			this.start = start;
			this.end = end;
			this.white = white;
			this.smooth = smooth;
			this.facelift = facelift;
			this.eye = eye;
		}


		public void run() {
			for(int i = start; i <= end; i++)
			{
				String tempImagePath = String.format(imagePath, i);
				String tempNewImagePath = String.format(newImagePath, i);
				String base = imagebase64Service.getImageBinary(tempImagePath);
				String temp = beautifyPicService.beautify_api("", "", base, white, smooth, facelift, eye);
				imagebase64Service.base64StringToImage(temp,tempNewImagePath);
			}
			latch.countDown();
		}
	}

	public void deleteFolder(String folderPath) {
		File file = new File(folderPath);
		if (file.exists()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		}
	}

}
