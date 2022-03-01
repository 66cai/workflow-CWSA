package cloud.workflowScheduling;
import java.io.IOException;
import java.util.*;

import cloud.workflowScheduling.setting.DeepCopyUtil;
import cloud.workflowScheduling.setting.VM;
class HashMapTest 
{
	public static void main(String[] args) throws ClassNotFoundException, IOException 
	{
		HashMap<VM,String> hm = new HashMap<VM,String>();

		//��Ҫ��֤���򡣴����˳���ȡ����˳��һ�¡�
		//��ʱ������һ���ڹ�ϣ���м�������ṹ��һ������LinkedHashMap���ö�����HashMap�����ࡣ
		hm = new LinkedHashMap<VM,String>();
		VM v1 = new VM(3);
		VM v2 = DeepCopyUtil.copy(v1);
		hm.put(new VM(0),"����");
		hm.put(new VM(1),"�Ϻ�");
		hm.put(v1,"����");
		hm.put(v2,"���");

//		hm.put(new Student("lisi4",24),"����");


		Set<Map.Entry<VM,String>> entrySet = hm.entrySet();

		Iterator<Map.Entry<VM,String>> it = entrySet.iterator();

		while(it.hasNext())
		{
			Map.Entry<VM,String> me = it.next();

			VM stu = me.getKey();
			String add = me.getValue();

			System.out.println(stu.getId()+"..."+stu.getType()+"....."+add);
		}
		
//		HashMap<Student,String> hm = new HashMap<Student,String>();

//		//��Ҫ��֤���򡣴����˳���ȡ����˳��һ�¡�
//		//��ʱ������һ���ڹ�ϣ���м�������ṹ��һ������LinkedHashMap���ö�����HashMap�����ࡣ
//		hm = new LinkedHashMap<Student,String>();
//
//		hm.put(new Student("lisi1",21),"����");
//		hm.put(new Student("lisi1",21),"�Ϻ�");
//		hm.put(new Student("lisi2",22),"����");
//		hm.put(new Student("lisi6",26),"���");
////		hm.put(new Student("lisi4",24),"����");
//
//
//		Set<Map.Entry<Student,String>> entrySet = hm.entrySet();
//
//		Iterator<Map.Entry<Student,String>> it = entrySet.iterator();
//
//		while(it.hasNext())
//		{
//			Map.Entry<Student,String> me = it.next();
//
//			Student stu = me.getKey();
//			String add = me.getValue();
//
//			System.out.println(stu.getName()+"..."+stu.getAge()+"....."+add);
//		}
	}
}

/*
Ҫ��֤ѧ�������Ψһ�ԡ���Ҫ����ѧ������������ж���ͬ�����ݡ�
����Ҫ����ѧ�����ж��������������ݡ�
��Ϊ�Ǵ����Hash���У�����Ҫ����hashCode��������equals������
*/
class Student
{
	private String name;
	private int age;
	Student(String name,int age)
	{
		this.name = name;
		this.age = age;
	}
	public int hashCode()
	{
		final int NUM = 23;
		return name.hashCode()+age*NUM;
	}
	public boolean equals(Object obj)
	{
		if(this==obj)
			return true;
		if(!(obj instanceof Student))
			return false;
		Student stu = (Student)obj;
		return this.name.equals(stu.name) && this.age==stu.age;
	}
	public String getName()
	{
		return name;
	}
	public int getAge()
	{
		return age;
	}
}
