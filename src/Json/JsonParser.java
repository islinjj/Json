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
	 * 开始解析
	 * 
	 * @return一个对象
	 */
	public Object parse() {
		CharRange newjson = newRange(0, json.length());
		return processValue(newjson);
	}

	/*
	 * 处理值，判断时对象还是数组或是字符串，null,true,false
	 * 如果是对象交给processObject处理
	 * 数组则交给processArray处理
	 * 字符串，null，true，false，数字->直接解析
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
	 * 处理数组与处理对象类似
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
	 * 处理对象
	 * 让processProperties函数来处理去掉{}之后的值
	 * 将name,value都存入map中
	 * 此处的forEach为简化后的for each
	 */
	private Object processObject(CharRange newjson) {
		// 交给processProperties处理去除{}后的内容
		List<Property> properties = processProperties(newRange(newjson.start + 1, newjson.end - 1));

		Map<String, Object> map = new HashMap<>();
		properties.forEach(pro -> map.put(pro.name, pro.value));
		return map;
	}

	/*
	 * 处理{}中的内容
	 * ：前面的为key，需要用一个变量来标记key开始的位置，当遇到：的之后就要进行记录，nameToken->key
	 * 处理key后，接着去找value，需要注意的是value可能是数组，对象等等，就交给findNextValue处理
	 * 此处还用到原子处理，可以保证在所有的函数都完成才继续进行下一步，不会有中断
	 */
	private List<Property> processProperties(CharRange newjson) {
		List<Property> properties = new ArrayList<>();
		int nameStartMark = newjson.start;
		for (int i = newjson.start; i < newjson.end; i++) {
			char ch = json.charAt(i);
			if (ch == ':') {
				CharRange nameToken = newRange(nameStartMark, i);
				// 处理：之后的嵌套
				AtomicInteger readCursor = new AtomicInteger();
				//++i是因为i此处是：的位置，需要+1
				CharRange valueSegment = findNextValue(newRange(++i, newjson.end), readCursor);

				//执行完findNextValue累加i，比如：findNextValue找到了一个“，”返回“，”的位置，此处的i就会更新
				i = readCursor.intValue() + 1;
				//更新nameStartMark
				nameStartMark = i;
				
				final String name = newRange(nameToken.start+1,nameToken.end-1).toString();
				//继续处理findNextValue()返回来的值然后赋给value
				final Object value = processValue(valueSegment);
				//加入列表当中，of()返回一个Property对象
				properties.add(Property.of(name,value));
			}
		}
		return properties;
	}

	/*
	 * 处理“：”之后的值
	 * 如果是“{”那就是对象处理
	 * 如果是“[”就是数组处理
	 * 否则就是找到“，”位置并返回“，”的位置，处理“，”前的类型
	 */
	private CharRange findNextValue(CharRange newRange, AtomicInteger readCursor) {
		CharRange range = newRange;
		if(range.returnChar(0)=='{') {
			//找到相匹配的{}或者[]
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
			//设置i的值
			readCursor.set(i);
			return newRange(range.start,i);
		}
	}

	/*
	 * 找到相互匹配的[]或{}
	 * 使用symbolsScore来标记遇到的{或[，如果symbolsScore=0说明匹配完成，使得valueSegment等于这对象或者数组
	 * 接着找到这个最里面的对象或者数组之间的数值，记录“，”出现的位置并设置i的值
	 */
	private CharRange pair(CharRange range, AtomicInteger readCursor, String symbolPair) {
		//分别匹配symbolPair的左符号和右符号
        int leftSymbol = symbolPair.charAt(0);
        int rightSymbol = symbolPair.charAt(1);
        //symbolsScore标记着出现的左符号，当进入此函数时已经有了第一个左符号
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
            //为0则说明匹配符号成对出现结束
            if (symbolsScore == 0) {
                valueSegment = newRange(range.start, i + 1);
                break;
            }
        }

        //左边符号~右边符号之间的内容就是需要的内容，记录{},或者[],后，的位置
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
	 * 更新对象
	 */
	CharRange newRange(int start, int end) {
		return new CharRange(start, end);
	}

	/*
	 * 处理对象的name,value
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

		// 判断null，true，false
		public boolean equalsString(String string) {
			//第一个true为忽略大小写
			return json.regionMatches(true, start, string, 0, string.length());
		}

		// 返回字符
		char returnChar(int index) {
			return json.charAt(index + start);
		}

		@Override
		public String toString() {
			return json.substring(start,end).toString();
		}
	}

}
