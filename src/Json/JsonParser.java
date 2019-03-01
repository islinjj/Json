package Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JsonParser {

	private final String json;

	public JsonParser(String json) {
		super();
		this.json = json;
	}

	/**
	 * ��ʼ����
	 * 
	 * @returnһ������
	 */
	public Object parse() {
		CharRange newjson = newRange(0, json.length());
		return processValue(newjson);
	}

	/*
	 * ����ֵ���ж�ʱ��������������ַ�����null,true,false
	 * ����Ƕ��󽻸�processObject����
	 * �����򽻸�processArray����
	 * �ַ�����null��true��false������->ֱ�ӽ���
	 */
	private Object processValue(CharRange newjson) {
		Object value;
		if (newjson.returnChar(0) == '{') {
			value = processObject(newjson);
		} else if (newjson.returnChar(0) == '[') {
			value = processArray(newjson);
		} else if (newjson.returnChar(0) == '"') {
			value = newRange(newjson.start + 1, newjson.end - 1);
		} else if (newjson.equalsString("null")) {
			value = null;
		} else if (newjson.equalsString("true")) {
			value = true;
		} else if (newjson.equalsString("false")) {
			value = false;
		} else {
			value = Double.parseDouble(newjson.toString());
		}
		return value;
	}

	/*
	 * ���������봦���������
	 */
	private List<?> processArray(CharRange newjson) {
		return processElements(newRange(newjson.start+1,newjson.end-1));
	}

	private List<?> processElements(CharRange newRange) {
		List<Object> array = new ArrayList<>();
		int elementStartMark = newRange.start;
		for(int i=newRange.start;i<newRange.end;i++) {
			AtomicInteger readCursor = new AtomicInteger();
            CharRange elementSegment = findNextValue(newRange(elementStartMark, newRange.end), readCursor);

            i=readCursor.intValue();
            elementStartMark = i+1;
            
            Object elementValue = processValue(elementSegment);
            array.add(elementValue);
		}
		return array;
	}

	/*
	 * �������
	 * ��processProperties����������ȥ��{}֮���ֵ
	 * ��name,value������map��
	 * �˴���forEachΪ�򻯺��for each
	 */
	private Object processObject(CharRange newjson) {
		// ����processProperties����ȥ��{}�������
		List<Property> properties = processProperties(newRange(newjson.start + 1, newjson.end - 1));

		Map<String, Object> map = new HashMap<>();
		properties.forEach(pro -> map.put(pro.name, pro.value));
		return map;
	}

	/*
	 * ����{}�е�����
	 * ��ǰ���Ϊkey����Ҫ��һ�����������key��ʼ��λ�ã�����������֮���Ҫ���м�¼��nameToken->key
	 * ����key�󣬽���ȥ��value����Ҫע�����value���������飬����ȵȣ��ͽ���findNextValue����
	 * �˴����õ�ԭ�Ӵ������Ա�֤�����еĺ�������ɲż���������һ�����������ж�
	 */
	private List<Property> processProperties(CharRange newjson) {
		List<Property> properties = new ArrayList<>();
		int nameStartMark = newjson.start;
		for (int i = newjson.start; i < newjson.end; i++) {
			char ch = json.charAt(i);
			if (ch == ':') {
				CharRange nameToken = newRange(nameStartMark, i);
				// ����֮���Ƕ��
				AtomicInteger readCursor = new AtomicInteger();
				//++i����Ϊi�˴��ǣ���λ�ã���Ҫ+1
				CharRange valueSegment = findNextValue(newRange(++i, newjson.end), readCursor);

				//ִ����findNextValue�ۼ�i�����磺findNextValue�ҵ���һ�����������ء�������λ�ã��˴���i�ͻ����
				i = readCursor.intValue() + 1;
				//����nameStartMark
				nameStartMark = i;
				
				final String name = newRange(nameToken.start+1,nameToken.end-1).toString();
				//��������findNextValue()��������ֵȻ�󸳸�value
				final Object value = processValue(valueSegment);
				//�����б��У�of()����һ��Property����
				properties.add(Property.of(name,value));
			}
		}
		return properties;
	}

	/*
	 * ��������֮���ֵ
	 * ����ǡ�{���Ǿ��Ƕ�����
	 * ����ǡ�[���������鴦��
	 * ��������ҵ�������λ�ò����ء�������λ�ã���������ǰ������
	 */
	private CharRange findNextValue(CharRange newRange, AtomicInteger readCursor) {
		CharRange range = newRange;
		if(range.returnChar(0)=='{') {
			//�ҵ���ƥ���{}����[]
			return pair(range,readCursor,"{}");
		}else if(range.returnChar(0)=='[') {
			return pair(range,readCursor,"[]");
		}else {
			int i;
			for(i=range.start+1;i<range.end;i++) {
				char ch = json.charAt(i);
				if(ch==',') {
					break;
				}
			}
			//����i��ֵ
			readCursor.set(i);
			return newRange(range.start,i);
		}
	}

	/*
	 * �ҵ��໥ƥ���[]��{}
	 * ʹ��symbolsScore�����������{��[�����symbolsScore=0˵��ƥ����ɣ�ʹ��valueSegment����������������
	 * �����ҵ����������Ķ����������֮�����ֵ����¼���������ֵ�λ�ò�����i��ֵ
	 */
	private CharRange pair(CharRange range, AtomicInteger readCursor, String symbolPair) {
		//�ֱ�ƥ��symbolPair������ź��ҷ���
        int leftSymbol = symbolPair.charAt(0);
        int rightSymbol = symbolPair.charAt(1);
        //symbolsScore����ų��ֵ�����ţ�������˺���ʱ�Ѿ����˵�һ�������
        int symbolsScore = 1;
        int i;
        CharRange valueSegment = null;
        for (i = range.start + 1; i < range.end; i++) {
            char ch = json.charAt(i);
            if (ch == leftSymbol) {
                symbolsScore++;
            } else if (ch == rightSymbol) {
                symbolsScore--;
            }
            //Ϊ0��˵��ƥ����ųɶԳ��ֽ���
            if (symbolsScore == 0) {
                valueSegment = newRange(range.start, i + 1);
                break;
            }
        }

        //��߷���~�ұ߷���֮������ݾ�����Ҫ�����ݣ���¼{},����[],�󣬵�λ��
        for (; i < range.end; i++) {
            char chx = json.charAt(i);
            if (chx == ',') {
                break;
            }
        }

        readCursor.set(i);
        return valueSegment;
	}

	/*
	 * ���¶���
	 */
	CharRange newRange(int start, int end) {
		return new CharRange(start, end);
	}

	/*
	 * ��������name,value
	 */
	static class Property {
		final String name;
		final Object value;

		public Property(String name, Object value) {
			super();
			this.name = name;
			this.value = value;
		}

		public static Property of(String name, Object value) {
			return new Property(name,value);
		}

	}

	class CharRange {

		final int start;
		final int end;

		public CharRange(int start, int end) {
			super();
			this.start = start;
			this.end = end;
		}

		// �ж�null��true��false
		public boolean equalsString(String string) {
			//��һ��trueΪ���Դ�Сд
			return json.regionMatches(true, start, string, 0, string.length());
		}

		// �����ַ�
		char returnChar(int index) {
			return json.charAt(index + start);
		}

		@Override
		public String toString() {
			return json.substring(start,end).toString();
		}
	}

}
