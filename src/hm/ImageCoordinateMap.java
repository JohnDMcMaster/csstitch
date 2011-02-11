/*
Copyright Christian Sattler <sattler.christian@gmail.com>
Modifications by John McMaster <JohnDMcMaster@gmail.com>
*/

package hm;
import java.util.*;
import general.collections.*;

/*
Eh this didn't work as well as I hoped
This article even went so far as to call it an antipattern,
although I still think that typedef is better than no typedef
http://www.ibm.com/developerworks/library/j-jtp02216.html
*/
public class ImageCoordinateMap
{
	public TreeMap<Pair<Integer, Integer>, String> m_map;

	public ImageCoordinateMap()
	{
		m_map = new TreeMap<Pair<Integer, Integer>, String>();
	}

	public ImageCoordinateMap(TreeMap<Pair<Integer, Integer>, String> map)
	{
		m_map = map;
	}
	
	public ImageCoordinateMap(ImageCoordinateMap other)
	{
		m_map = (TreeMap<Pair<Integer, Integer>, String>)other.m_map.clone();
	}

	//By far the most common used method of m_map
	public int size()
	{
		return m_map.size();
	}
}

