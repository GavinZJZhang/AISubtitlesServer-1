# -*- coding: utf-8 -*-
from tencentcloud.common import credential
from tencentcloud.common.profile.client_profile import ClientProfile
from tencentcloud.common.profile.http_profile import HttpProfile
from tencentcloud.common.exception.tencent_cloud_sdk_exception import TencentCloudSDKException
from tencentcloud.asr.v20190614 import asr_client, models

from sys import argv

import base64
import json
import re

ID = ""
KEY = ""


class audio2text(object):

    def __init__(self):
        self.cred = credential.Credential(ID, KEY)
        self.httpProfile = HttpProfile()
        self.httpProfile.endpoint = "asr.tencentcloudapi.com"

        self.clientProfile = ClientProfile()
        self.clientProfile.httpProfile = self.httpProfile
        self.clientProfile.signMethod = "TC3-HMAC-SHA256"  # 签名方法v3
        self.client = asr_client.AsrClient(self.cred, "", self.clientProfile)

    # 发送请求
    def a2t(self, audioFilePath):
        try:
            # 读取文件以及转base64
            audioFile = open(audioFilePath, mode='rb')
            data = audioFile.read()
            dataLen = len(data)
            base64Data = base64.b64encode(data).decode()

            req = models.CreateRecTaskRequest()
            params = {"EngineModelType": "16k_zh_video", "ChannelNum": 1, "ResTextFormat": 0, "SourceType": 1,
                      "Data": base64Data, "DataLen": dataLen}
            req._deserialize(params)
            resp = self.client.CreateRecTask(req)
            resp = resp.to_json_string()  # 转换为json格式

            resp = json.loads(resp)  # 转换为字典，以便提取TaskId
            taskId = resp["Data"]["TaskId"]

            return taskId

        except TencentCloudSDKException as err:
            print(err)

    # 查询结果
    def getText(self, taskId):
        try:
            req = models.DescribeTaskStatusRequest()
            params = '{"TaskId":' + str(taskId) + '}'
            req.from_json_string(params)

            resp = self.client.DescribeTaskStatus(req)
            # print(resp.to_json_string())

            return resp.to_json_string()

        except TencentCloudSDKException as err:
            print(err)


def formattime2time(ft):
    hh, mm, ss, xxx = re.split(':|,', ft)
    t = int(xxx) + int(ss) * 1000 + int(mm) * \
        1000 * 60 + int(hh) * 1000 * 60 * 60
    return t


def time2formattime(t):
    xxx = t % 1000
    t -= xxx
    ss = t % (1000 * 60)
    t -= ss
    mm = t % (1000 * 60 * 60)
    t -= mm
    hh = t % (1000 * 60 * 60 * 60)
    return "%02d:%02d:%02d,%03d" % (int(hh / (1000 * 60 * 60)), int(mm / (1000 * 60)), int(ss / 1000), xxx)

# 将时间转为hh:mm:ss格式
def transToTime(strTime):
    time = strTime.split(":")
    hh = int(time[0]) if len(time) == 3 else 0
    mm = int(time[1]) if len(time) == 3 else (
        int(time[0]) if len(time) == 2 else 0)
    _ss = time[len(time) - 1].split(".")
    ss = int(_ss[0])
    ms = int(_ss[1])
    return "%02d:%02d:%02d,%03d" % (hh, mm, ss, ms)


# 根据音频得到识别结果，处理并输出srt格式的中文字幕
def getZhSubtitle(audioPath, zhFilePath):
    # 发送识别请求
    testA2T = audio2text()
    myTaskId = testA2T.a2t(audioPath)
    myResult = ''
    cnt = 0
    # 查询识别结果
    while 1:
        if ++cnt % 1000000000 == 0:
            myText = json.loads(testA2T.getText(myTaskId))
            myStatus = myText["Data"]["Status"]
            if myStatus == 2:
                myResult = myText["Data"]["Result"]
                break

    res = myResult.split("\n")
    res.pop()
    # 按srt格式写入文档
    with open(zhFilePath, 'w+', encoding='utf-8') as newFile:
        step = 1
        for i in range(len(res)):
            temp = res[i].split('  ')
            t = temp[0][1:len(temp[0]) - 1].split(',')
            begin = transToTime(t[0])
            end = transToTime(t[1])
            text = temp[1]

            cnt = len(text)
            texts = text.split('，')
            cur_text = texts[0]
            btime = formattime2time(begin)
            diff = formattime2time(end) - btime
            sig_step = int(diff / cnt)
            cur_time = int(len(cur_text) * diff / cnt) + sig_step
            for j in range(1, len(texts)):
                if len(cur_text) + len(texts[j]) <= 24:
                    cur_text += "，" + texts[j]
                    cur_time += int(len(texts[j]) * diff / cnt) + sig_step
                else:
                    newFile.write(str(step) + '\n')
                    step += 1
                    newFile.write(time2formattime(btime) + ' --> ' +
                                  time2formattime(btime + cur_time) + '\n')
                    btime += cur_time
                    newFile.write(cur_text + '\n\n')
                    cur_text = texts[j]
                    cur_time = int(len(texts[j]) * diff / cnt) + sig_step
            newFile.write(str(step) + '\n')
            step += 1
            newFile.write(time2formattime(btime) + ' --> ' + end + '\n')
            newFile.write(cur_text)
            newFile.write('\n\n')
        newFile.close()


if __name__ == '__main__':
    audioPath = argv[1]  # 音频路径
    zhFilePath = argv[2]  # 中文字幕
    getZhSubtitle(audioPath, zhFilePath)
