package memory;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.List;

import sun.misc.Unsafe;

import com.javamex.classmexer.MemoryUtil;

/**
 * This class is for verification purposes only and the code is specific to use
 * cases explained on the associated blog entry.
 * 
 * Duplication of code is deliberate to help reduce the navigation across
 * methods. In a future version, I'd refactor it.
 * 
 * This code requires:
 * 
 * 1. Use of sun.misc.Unsafe and 2. classmexer.jar (to compute object sizes) and
 * 3. following VM props:
 * 
 * -javaagent:classmexer.jar (coz classmexer implements a Java instrumentation
 * agent) -XX:-UseCompressedOops (we'd experiment with compressed pointers on a
 * 64 bit VM)
 * 
 * How to run and test: ----------------------
 * 
 * 1. Put the jar in class path and working dir of Java project (project root)
 * 2. Import sun.misc.Unsafe and visit
 * http://stackoverflow.com/questions/860187/
 * access-restriction-on-class-due-to-restriction-on-required-library-rt-jar
 * 
 * Credits:
 * 
 * All these experts have helped me understand Java object layout better.
 * 
 * http://www.javamex.com/classmexer/
 * http://www.ibm.com/developerworks/opensource/library/j-codetoheap/index.html
 * http://javadetails.com/2012/03/21/java-array-memory-allocation.html
 * 
 * @author Nitin Singh (nitinsg1981@gmail.com)
 * @Date: Jan 26, 2014 (on a sunny breezy Sunday)
 */
public class JavaObjectLayout
{
    public static void main( String[ ] args ) throws Exception
    {

	//
	// Find if we are running a 32-bit or 64 bit VM (thats an important
	// factor in header size calc)
	//
	String vmBitSize = System.getProperty( "os.arch" );
	boolean is32BitVM = true;
	if ( "32".equalsIgnoreCase( vmBitSize ) || "x86".equalsIgnoreCase( vmBitSize ) )
	    System.out.println( "Using 32-bit JVM" );
	else
	{
	    is32BitVM = false;
	    System.out.println( "Using 64-bit JVM" );
	}

	//
	// Check if runtime is using compressed pointers on 64 bit VM
	//
	boolean usingCompressedOops = true;

	if ( is32BitVM )
	    usingCompressedOops = false;
	else
	{

	    List< String > vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
	    System.out.println( "VM args:" );
	    for ( String arg : vmArgs )
		System.out.println( arg );

	    // By default a 64-bit VM uses compressed oops. That setting can
	    // also be explicitly enabled
	    // via -XX:+UseCompressedOops or disabled via -XX:-UseCompressedOops
	    usingCompressedOops = vmArgs.contains( USE_COMPRESSED_OOPS );
	    if ( ! usingCompressedOops && vmArgs.contains( DONT_USE_COMPRESSED_OOPS ) )
		usingCompressedOops = false;
	    else
		// enabled by default
		usingCompressedOops = true;
	}

	System.out.println( "Using compressed OOPS ? " + ( usingCompressedOops ? "Yes" : "No" ) );

	//
	// Size of a reference
	//
	Field field = sun.misc.Unsafe.class.getDeclaredField( "theUnsafe" );
	field.setAccessible( true );
	sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get( null );
	System.out.println( "Address size = " + unsafe.addressSize() );

	dump_Integer_Layout( unsafe, is32BitVM, usingCompressedOops );

	dump_Double_Layout( unsafe, is32BitVM, usingCompressedOops );

	dump_ShortArray_Layout( unsafe, is32BitVM, usingCompressedOops );

	dump_String_Layout( unsafe, is32BitVM, usingCompressedOops );

	dump_InheritedObject_Layout1( unsafe, is32BitVM, usingCompressedOops );

	dump_InheritedObject_Layout2( unsafe, is32BitVM, usingCompressedOops );

	dump_InheritedObject_Layout3( unsafe, is32BitVM, usingCompressedOops );

	dump_Static_Inner_Class_Layout( unsafe, is32BitVM, usingCompressedOops );

	dump_Non_Static_Inner_Class_Layout( unsafe, is32BitVM, usingCompressedOops );

    }

    private static void dump_Integer_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Integer obj = new Integer( 222 );

	printObjectSize( "java.lang.Integer", obj );
	System.out.println( "Shallow and deep layout for an Integer object are same." );
	if ( is32BitVM )
	{
	    // Rule 1
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 8 ) );
	    System.out.println( PADDING_PREFIX + "padding 4 bytes to allow integer object to end at an 8 byte boundary." );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to allow integer object to end at an 8 byte boundary." );
	    }
	}
    }

    private static void dump_Double_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Double obj = new Double( 4567889922.23344 );

	printObjectSize( "java.lang.Double", obj );
	System.out.println( "Shallow and deep layout for java.lang.Double object are same." );
	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 8 ) );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to align double value to 8 byte boundary" );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );

	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
	    }
	}
    }

    private static void dump_ShortArray_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	short[ ] obj = new short [ ] { 23, 54, 7 };

	// Using instrumentation
	printObjectSize( "short[] array", obj );
	System.out.println( "-- short[] heap layout --" );
	if ( is32BitVM )
	{
	    // takes 12 bytes
	    printHeader( obj, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    // member offsets within array
	    System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 12 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 14 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 16 ) );
	    System.out.println( PADDING_PREFIX + "padding 6 bytes to align array object to 8 bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// takes 16 bytes
		printHeader( obj, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		// member offsets within array
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 18 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 20 ) );
		System.out.println( PADDING_PREFIX + "padding 2 bytes to align array object to 8 bytes boundary" );
	    }
	    else
	    {
		// takes 24 bytes
		printHeader( obj, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		// member offsets within array
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 24 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 26 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getShort( obj, 28 ) );
		System.out.println( PADDING_PREFIX + "padding 2 bytes to align array object to 8 bytes boundary" );
	    }
	}
    }

    private static void dump_String_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops ) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
	String obj = "enlightenment";

	// Using instrumentation
	printObjectSize( "java.lang.String", obj );

	Field hash32 = String.class.getDeclaredField( "hash32" );
	hash32.setAccessible( true );
	System.out.println( String.format( "String is --> char[]: %s, hashCode:%d, hash32:%d", obj, obj.hashCode(), hash32.get( obj ) ) );

	/*
	 * Java 7 strings have 2 primitive integers fields viz hash and hash32
	 * and one char[] array for data
	 */
	// Using instrumentation
	System.out.println( "-- Shallow layout --" );
	dump_String_ShallowLayout( obj, unsafe, is32BitVM, usingCompressedOops );
	System.out.println( "-- Deep layout --" );
	dump_String_DeepLayout( obj, unsafe, is32BitVM, usingCompressedOops );

    }

    private static void dump_String_DeepLayout( String obj, Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops ) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
	// array header takes 12 bytes
	char[ ] dataArray = new char [ 0 ];
	Field f = String.class.getDeclaredField( "value" );
	f.setAccessible( true );
	dataArray = (char[ ]) f.get( obj );

	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
	    System.out.println( MEMBER_PREFIX + "member char[] array ref size (4 bytes): 0x" + Integer.toHexString( unsafe.getInt( obj, 16 ) ) );

	    System.out.println( "..member char[] array starts .." );

	    // array header takes 12 bytes
	    printHeader( dataArray, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

	    // member offsets within the array
	    System.out.println( MEMBER_PREFIX + "data[0] = " + unsafe.getChar( dataArray, 12 ) );
	    System.out.println( MEMBER_PREFIX + "data[1] = " + unsafe.getChar( dataArray, 14 ) );
	    System.out.println( MEMBER_PREFIX + "data[2] = " + unsafe.getChar( dataArray, 16 ) );
	    System.out.println( MEMBER_PREFIX + "data[3] = " + unsafe.getChar( dataArray, 18 ) );
	    System.out.println( MEMBER_PREFIX + "data[4] = " + unsafe.getChar( dataArray, 20 ) );
	    System.out.println( MEMBER_PREFIX + "data[5] = " + unsafe.getChar( dataArray, 22 ) );
	    System.out.println( MEMBER_PREFIX + "data[6] = " + unsafe.getChar( dataArray, 24 ) );
	    System.out.println( MEMBER_PREFIX + "data[7] = " + unsafe.getChar( dataArray, 26 ) );
	    System.out.println( MEMBER_PREFIX + "data[8] = " + unsafe.getChar( dataArray, 28 ) );
	    System.out.println( MEMBER_PREFIX + "data[9] = " + unsafe.getChar( dataArray, 30 ) );
	    System.out.println( MEMBER_PREFIX + "data[10] = " + unsafe.getChar( dataArray, 32 ) );
	    System.out.println( MEMBER_PREFIX + "data[11] = " + unsafe.getChar( dataArray, 34 ) );
	    System.out.println( MEMBER_PREFIX + "data[12] = " + unsafe.getChar( dataArray, 36 ) );
	    System.out.println( PADDING_PREFIX + "2 bytes padding to allow char[] to end at an 8-bytes boundary." );

	    System.out.println( PADDING_PREFIX + "padding 4 bytes to align String object to end at an 8-bytes boundary" );

	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// Rule 8
		// header takes 12
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + "member char[] array ref size (4 bytes): 0x" + Integer.toHexString( unsafe.getInt( obj, 16 ) ) );

		System.out.println( "..member char[] array starts.. " );

		// array header takes 16 bytes
		printHeader( dataArray, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// member offsets within the array
		System.out.println( MEMBER_PREFIX + "data[0] = " + unsafe.getChar( dataArray, 16 ) );
		System.out.println( MEMBER_PREFIX + "data[1] = " + unsafe.getChar( dataArray, 18 ) );
		System.out.println( MEMBER_PREFIX + "data[2] = " + unsafe.getChar( dataArray, 20 ) );
		System.out.println( MEMBER_PREFIX + "data[3] = " + unsafe.getChar( dataArray, 22 ) );
		System.out.println( MEMBER_PREFIX + "data[4] = " + unsafe.getChar( dataArray, 24 ) );
		System.out.println( MEMBER_PREFIX + "data[5] = " + unsafe.getChar( dataArray, 26 ) );
		System.out.println( MEMBER_PREFIX + "data[6] = " + unsafe.getChar( dataArray, 28 ) );
		System.out.println( MEMBER_PREFIX + "data[7] = " + unsafe.getChar( dataArray, 30 ) );
		System.out.println( MEMBER_PREFIX + "data[8] = " + unsafe.getChar( dataArray, 32 ) );
		System.out.println( MEMBER_PREFIX + "data[9] = " + unsafe.getChar( dataArray, 34 ) );
		System.out.println( MEMBER_PREFIX + "data[10] = " + unsafe.getChar( dataArray, 36 ) );
		System.out.println( MEMBER_PREFIX + "data[11] = " + unsafe.getChar( dataArray, 38 ) );
		System.out.println( MEMBER_PREFIX + "data[12] = " + unsafe.getChar( dataArray, 40 ) );
		System.out.println( PADDING_PREFIX + "padding 6 bytes to allow char[] array to end at an 8 bytes boundary" );
	    }
	    else
	    {
		// Rule 1 and 8
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 20 ) );
		System.out.println( MEMBER_PREFIX + "member char[] array ref size (8 bytes): 0x" + Long.toHexString( unsafe.getLong( obj, 24 ) ) );

		System.out.println( ".. member char[] array starts .. " );

		// array header takes 24 bytes
		printHeader( dataArray, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// member offsets within the array
		System.out.println( MEMBER_PREFIX + "data[0] = " + unsafe.getChar( dataArray, 24 ) );
		System.out.println( MEMBER_PREFIX + "data[1] = " + unsafe.getChar( dataArray, 26 ) );
		System.out.println( MEMBER_PREFIX + "data[2] = " + unsafe.getChar( dataArray, 28 ) );
		System.out.println( MEMBER_PREFIX + "data[3] = " + unsafe.getChar( dataArray, 30 ) );
		System.out.println( MEMBER_PREFIX + "data[4] = " + unsafe.getChar( dataArray, 32 ) );
		System.out.println( MEMBER_PREFIX + "data[5] = " + unsafe.getChar( dataArray, 34 ) );
		System.out.println( MEMBER_PREFIX + "data[6] = " + unsafe.getChar( dataArray, 36 ) );
		System.out.println( MEMBER_PREFIX + "data[7] = " + unsafe.getChar( dataArray, 38 ) );
		System.out.println( MEMBER_PREFIX + "data[8] = " + unsafe.getChar( dataArray, 40 ) );
		System.out.println( MEMBER_PREFIX + "data[9] = " + unsafe.getChar( dataArray, 42 ) );
		System.out.println( MEMBER_PREFIX + "data[10] = " + unsafe.getChar( dataArray, 44 ) );
		System.out.println( MEMBER_PREFIX + "data[11] = " + unsafe.getChar( dataArray, 46 ) );
		System.out.println( MEMBER_PREFIX + "data[12] = " + unsafe.getChar( dataArray, 48 ) );
		System.out.println( PADDING_PREFIX + "padding 6 bytes to allow member char[] array to end at an 8 bytes boundary" );
	    }
	}

    }

    private static void dump_String_ShallowLayout( String obj, Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
	    System.out.println( MEMBER_PREFIX + "member char[] array ref size (4 bytes): 0x" + unsafe.getInt( obj, 16 ) );
	    System.out.println( PADDING_PREFIX + "4 bytes padding to allow string to end at an 8-bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + "member char[] array ref size (4 bytes): 0x" + Integer.toHexString( unsafe.getInt( obj, 20 ) ) );
	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 20 ) );
		System.out.println( MEMBER_PREFIX + "member char[] array ref size (8 bytes): 0x" + Long.toHexString( unsafe.getLong( obj, 24 ) ) );
	    }
	}
    }

    private static void dump_InheritedObject_Layout1( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Subclass1 obj = new Subclass1();
	printObjectSize( "Subclass1", obj );

	System.out.println( "-- Heap layout --" );

	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 8 ) );
	    System.out.println( "..subclass starts next..." );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 24 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 28 ) );
	    System.out.println( PADDING_PREFIX + "3 bytes to align entire object to an 8-bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// Rule 2 and 4 applied
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to align for double in super class" );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
		System.out.println( "..subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 24 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 32 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 36 ) );
		System.out.println( PADDING_PREFIX + "padding 3 bytes to align entire object to an 8-bytes boundary" );
	    }
	    else
	    {
		// Rule 4 applied
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
		System.out.println( "..subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 24 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 32 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 36 ) );
		System.out.println( PADDING_PREFIX + "padding 3 bytes to align entire object to an 8-bytes boundary" );
	    }
	}
    }

    private static void dump_InheritedObject_Layout2( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Subclass2 obj = new Subclass2();
	printObjectSize( "Subclass2", obj );

	System.out.println( "-- Heap layout --" );

	if ( is32BitVM )
	{
	    // Rule 4 applied
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 8 ) );
	    System.out.println( PADDING_PREFIX + "padding 3 bytes to align for double in subclass" );
	    System.out.println( "..subclass starts next..." );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// Rule 4 applied
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 12 ) );
		System.out.println( PADDING_PREFIX + "padding 3 bytes to align for double in subclass" );
		System.out.println( "..subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 24 ) );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to align entire object to an 8-bytes boundary" );
	    }
	    else
	    {
		// Rule 4 applied
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getBoolean( obj, 16 ) );
		System.out.println( PADDING_PREFIX + "padding 7 bytes to align for double in subclass" );
		System.out.println( "..subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 24 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 32 ) );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to align entire object to an 8-bytes boundary" );
	    }
	}
    }

    private static void dump_InheritedObject_Layout3( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Subclass3 obj = new Subclass3();
	printObjectSize( "Subclass3", obj );

	System.out.println( "-- Heap layout --" );

	if ( is32BitVM )
	{
	    // Rule 4 applied
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( ".. no member in super class, subclass starts next..." );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
	    System.out.println( PADDING_PREFIX + "4 bytes to align object to 8 bytes boundary." );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// Rule 5 and 6 applied
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( ".. no member in super class, subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
	    }
	    else
	    {
		// Rule 4 applied
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( ".. no member in super class, subclass starts next..." );
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 24 ) );
		System.out.println( PADDING_PREFIX + "padding 4 bytes to align entire object to an 8-bytes boundary" );
	    }
	}
    }

    private static void dump_Static_Inner_Class_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Static_Inner_Class obj = new Static_Inner_Class();

	// Using instrumentation
	printObjectSize( "Static Inner Class", obj );

	// Using instrumentation
	System.out.println( "-- Shallow layout --" );
	dump_Static_Inner_Class_ShallowLayout( obj, unsafe, is32BitVM, usingCompressedOops );
	System.out.println( "-- Deep layout --" );
	dump_Static_Inner_Class_DeepLayout( obj, unsafe, is32BitVM, usingCompressedOops );

    }

    private static void dump_Static_Inner_Class_DeepLayout( Static_Inner_Class obj, Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 20 ) );
	    System.out.println( MEMBER_PREFIX + "short[] ref (4 bytes):  0x" + Integer.toHexString( unsafe.getInt( obj, 28 ) ) );

	    System.out.println( "..member short[] array starts.. " );
	    // array header takes 12 bytes
	    printHeader( obj.arr, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    // menber offsets within the array
	    System.out.println( MEMBER_PREFIX + "arr[0] = " + unsafe.getShort( obj.arr, 14 ) );
	    System.out.println( MEMBER_PREFIX + "arr[1] = " + unsafe.getShort( obj.arr, 16 ) );
	    System.out.println( PADDING_PREFIX + "4 bytes padding to allow the object to end at an 8 bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// int comes first in this case. Because VM was smart enough
		// to use the gap before double's 8 byte boundary using the int
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );

		// then double
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );

		// then float
		System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 24 ) );

		System.out.println( MEMBER_PREFIX + "short[] ref (4 bytes):  0x" + Integer.toHexString( unsafe.getInt( obj, 28 ) ) );

		System.out.println( "..member short[] array starts.. " );
		printHeader( obj.arr, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + "arr[0] = " + unsafe.getShort( obj.arr, 16 ) );
		System.out.println( MEMBER_PREFIX + "arr[1] = " + unsafe.getShort( obj.arr, 18 ) );
		System.out.println( PADDING_PREFIX + "4 bytes padding to allow the short[] array end at an 8 bytes boundary" );
	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );// returns

		// double member
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );

		// then int member
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 24 ) );

		// then float member
		System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 28 ) );

		System.out.println( MEMBER_PREFIX + "short[] ref (8 bytes):  0x" + Long.toHexString( unsafe.getLong( obj, 32 ) ) );

		System.out.println( "..member short[] array starts.. " );

		printHeader( obj.arr, unsafe, THIS_IS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// member offset within array
		System.out.println( MEMBER_PREFIX + "arr[0] = " + unsafe.getShort( obj.arr, 24 ) );

		// member offset within array
		System.out.println( MEMBER_PREFIX + "arr[1] = " + unsafe.getShort( obj.arr, 26 ) );

		System.out.println( PADDING_PREFIX + "4 bytes padding to allow the short[] array object to end at an 8 bytes boundary" );

	    }
	}

    }

    private static void dump_Static_Inner_Class_ShallowLayout( Static_Inner_Class obj, Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

	    System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
	    System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 20 ) );
	    System.out.println( MEMBER_PREFIX + "arr-ref (4 bytes ref) : 0x" + Integer.toHexString( unsafe.getInt( obj.arr, 24 ) ) );
	    System.out.println( PADDING_PREFIX + "4 bytes padding to allow the obejct end at an 8 bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// int comes first because VM was smart enough
		// to use the gap before double's 8 byte boundary using the int
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );

		// then double
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );

		// then float
		System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 24 ) );
		System.out.println( MEMBER_PREFIX + "arr-ref (4 bytes ref) : " + Integer.toHexString( unsafe.getInt( obj.arr, 28 ) ) );
	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );

		// double member
		System.out.println( MEMBER_PREFIX + unsafe.getDouble( obj, 16 ) );

		// then int member
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 24 ) );

		// then float member
		System.out.println( MEMBER_PREFIX + unsafe.getFloat( obj, 28 ) );

		// member offset within array
		// long members are 8 byte aligned,
		// hence padding applied before long
		System.out.println( MEMBER_PREFIX + "arr-ref (8 bytes ref) : 0x" + Long.toHexString( unsafe.getLong( obj.arr, 32 ) ) );
	    }
	}

    }

    private static void dump_Non_Static_Inner_Class_Layout( Unsafe unsafe, boolean is32BitVM, boolean usingCompressedOops )
    {
	Non_Static_Inner_Class obj = new JavaObjectLayout().new Non_Static_Inner_Class();

	// Using instrumentation
	printObjectSize( "Non-Static Inner Class", obj );

	if ( is32BitVM )
	{
	    // header takes 8 bytes
	    printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
	    System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 8 ) );
	    System.out.println( MEMBER_PREFIX + "enclosing-context-ref (4 bytes) : 0x" + Integer.toHexString( unsafe.getInt( obj, 16 ) ) );
	    System.out.println( PADDING_PREFIX + "4 bytes padding to allow the obejct end at an 8 bytes boundary" );
	}
	else
	{
	    if ( usingCompressedOops )
	    {
		// header takes 12 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 12 ) );
		System.out.println( MEMBER_PREFIX + "enclosing-context-ref (4 bytes) : 0x" + Integer.toHexString( unsafe.getInt( obj, 16 ) ) );
		System.out.println( PADDING_PREFIX + "4 bytes padding to allow the obejct end at an 8 bytes boundary" );
	    }
	    else
	    {
		// header takes 16 bytes
		printHeader( obj, unsafe, IS_THIS_AN_ARRAY_OBJECT_HEADER, is32BitVM, usingCompressedOops );
		System.out.println( MEMBER_PREFIX + unsafe.getInt( obj, 16 ) );
		System.out.println( PADDING_PREFIX + "4 bytes padding to allow the enclosing-context-ref to begin at an 8 bytes boundary." );
		System.out.println( MEMBER_PREFIX + "enclosing-context-ref (8 bytes) : 0x" + Integer.toHexString( unsafe.getInt( obj, 16 ) ) );
	    }
	}

    }

    private static long printHeader( Object obj, Unsafe unsafe, boolean isArrayObject, boolean is32BitVM, boolean usingCompressedOops )
    {

	if ( is32BitVM )
	{
	    if ( isArrayObject )
	    {
		//
		// array header is 12 bytes on a 32 bit VM
		//
		System.out.println( "-- Array header total 12 bytes --" );
		System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 0 ) ) ) );
		System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 4 ) ) ) );
		System.out.println( String.format( "%s %s %s %s", HEADER_PREFIX, ARR_SIZE_HEADER, _4_BYTES_, unsafe.getInt( obj, 8 ) ) );
		return 12;
	    }
	    else
	    {
		//
		// non-array header is 8 bytes on a 32 bit VM
		//
		System.out.println( "-- Object header total 8 bytes --" );
		System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 0 ) ) ) );
		System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 4 ) ) ) );
		return 8;
	    }
	}
	else
	{
	    // 64 bit VM

	    if ( isArrayObject )
	    {
		if ( usingCompressedOops )
		{
		    //
		    // array header is 16 bytes on a 64 bit VM with compressed
		    // oops
		    //
		    System.out.println( "-- Array header total 16 bytes --" );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 0 ) ) ) );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 8 ) ) ) );
		    System.out.println( String.format( "%s %s %s %s", HEADER_PREFIX, ARR_SIZE_HEADER, _4_BYTES_, unsafe.getInt( obj, 12 ) ) );
		    return 16;
		}
		else
		{
		    //
		    // array header is 24 bytes on a 64 bit VM without
		    // compressed oops
		    //
		    System.out.println( "-- Array header total 24 bytes --" );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 0 ) ) ) );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 8 ) ) ) );
		    System.out.println( String.format( "%s %s %s %s", HEADER_PREFIX, ARR_SIZE_HEADER, _8_BYTES_, unsafe.getLong( obj, 16 ) ) );
		    return 24;
		}
	    }
	    else
	    {
		if ( usingCompressedOops )
		{
		    //
		    // non-array header is 12 bytes on a 64 bit VM with
		    // compressed oops
		    //
		    System.out.println( "-- Object header total 12 bytes --" );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 0 ) ) ) );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _4_BYTES_, Integer.toHexString( unsafe.getInt( obj, 8 ) ) ) );
		    return 12;
		}
		else
		{
		    //
		    // non-array header is 16 bytes on a 64 bit VM without
		    // compressed oops
		    //
		    System.out.println( "-- Object header total 16 bytes --" );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, MARK_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 0 ) ) ) );
		    System.out.println( String.format( "%s %s %s 0x%s", HEADER_PREFIX, KLASS_REF_HEADER, _8_BYTES_, Long.toHexString( unsafe.getLong( obj, 8 ) ) ) );
		    return 16;
		}
	    }
	}
    }

    private static void printObjectSize( String id, Object obj )
    {
	System.out.println( "\n------ " + id + " -----" );
	System.out.println( "Shallow size: " + MemoryUtil.memoryUsageOf( obj ) );
	System.out.println( "Deep size: " + MemoryUtil.deepMemoryUsageOf( obj ) );

    }

    private static class SuperClass1
    {
	double d1 = 101.404;
    }

    private static class Subclass1 extends SuperClass1
    {
	int i1 = 711;
	double d2 = 202.505;
	byte b1 = 2;
    }

    private static class SuperClass2
    {
	boolean b1 = true;
	// boolean b2 = false;
	// boolean b3 = true;
    }

    private static class Subclass2 extends SuperClass2
    {
	int i1 = 711;
	double d2 = 202.505;
    }

    private static class SuperClass3
    {
	// boolean b1 = true;
	// boolean b2 = false;
	// boolean b3 = true;
    }

    private static class Subclass3 extends SuperClass3
    {
	int i1 = 711;
	double d2 = 202.505;
    }

    private static class Static_Inner_Class
    {
	int i1 = 8179;
	float f1 = 12.35f;
	double d1 = 201;
	short arr[] = new short [ ] { 19, 91 };
    }

    private class Non_Static_Inner_Class
    {
	int i1 = 8179;
    }

    private static final String USE_COMPRESSED_OOPS = "-XX:+UseCompressedOops";
    private static final String DONT_USE_COMPRESSED_OOPS = "-XX:-UseCompressedOops";

    private static final String HEADER_PREFIX = "<H>: ";
    private static final String MEMBER_PREFIX = "<M>: ";
    private static final String PADDING_PREFIX = "<P>: ";
    private static final String MARK_HEADER = "Mark: ";

    private static final String KLASS_REF_HEADER = "Class-Ref: ";
    private static final String ARR_SIZE_HEADER = "Array Size: ";

    private static final String _8_BYTES_ = " (= 8 bytes) ";
    private static final String _4_BYTES_ = " (= 4 bytes) ";

    private static final boolean THIS_IS_AN_ARRAY_OBJECT_HEADER = true;
    private static final boolean IS_THIS_AN_ARRAY_OBJECT_HEADER = false;

}
