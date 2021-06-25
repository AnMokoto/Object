import os
import glob
import re

import pandas as pd
import xml.etree.ElementTree as ET


def xml_to_csv(path, output):
    xml_list = []
    # 读取注释文件
    for xml_file in glob.glob(path + '/*.xml'):
        tree = ET.parse(xml_file)
        root = tree.getroot()
        pub = (
            root.find('filename').text,
            int(root.find('size')[0].text),
            int(root.find('size')[1].text)
        )
        for member in root.findall('object'):
            value = (*pub,
                     member[0].text,
                     int(member[4][0].text),
                     int(member[4][1].text),
                     int(member[4][2].text),
                     int(member[4][3].text)
                     )
            xml_list.append(value)
    column_name = ['filename', 'width', 'height', 'class', 'xmin', 'ymin', 'xmax', 'ymax']

    # 将所有数据分为样本集和验证集，一般按照3:1的比例
    # train_list = xml_list[0: int(len(xml_list) * 0.67)]
    # eval_list = xml_list[int(len(xml_list) * 0.67) + 1:]
    data_list = xml_list[:]

    # 保存为CSV格式
    data_df = pd.DataFrame(data_list, columns=column_name)
    data_df.to_csv(output, index=None)


def _xml_to_csv(path, output, width=100, heith=100, name='apple'):
    data_list = []

    for file in os.listdir(path):
        p = os.path.join(path, file)
        if os.path.isdir(p):
            data_list += _xml_to_csv(p, output, width, heith, name)
        elif os.path.isfile(p) and re.match(r".+\.jpg$", file):
            filename = os.path.split(path)[1] + "/" + file
            data_list.append(
                (filename, width, heith, name,
                 0, 0, 100, 100
                 )
            )
    return data_list


def __xml_to_csv(path, output, width=100, heith=100, name='apple'):
    """
    for test csv
    """
    data_list = _xml_to_csv(path, output, width, heith, name)
    column_name = ['filename', 'width', 'height', 'class', 'xmin', 'ymin', 'xmax', 'ymax']
    pd.DataFrame(data_list, columns=column_name).to_csv(output, index=None)


def main():
    __xml_to_csv("fruits-360/Training", 'annotations/train.csv')
    __xml_to_csv("fruits-360/Test", 'annotations/testing.csv')
