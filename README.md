# android-swipelistview

An Android List View implementation with support for drawable cells and many other swipe related features

- [Introduction](#introduction)
- [Download](#download)
  - [Maven Dependency](#maven-dependency)
	- [APKLib and others](#apklib-and-others)
	- [Dependencies](#dependencies)
- [XML Usage](#xml-usage)

# Introduction

SwipeListView was born out of the need to add swipe gestures to ListView on Android for 
@ [47 Degrees](http://47deg.com) Clients. Contributions and constructive feedback are welcome.

# Download

## Maven Dependency

SwipeListView may be automatically imported into your project if you already use [Maven](http://maven.apache.org/). 
Just declare android-swipelistview as a maven dependency.
If you wish to always use the latest unstable snapshots, add the Sonatype repository where the SwipeListView 
snapshot artifacts are being deployed.
SwipeListView official releases will be made available at Maven Central.

```xml
<repository>
    <id>sonatype</id>
    <url>https://oss.sonatype.org/content/groups/public/</url>
    <releases>
        <enabled>true</enabled>
        <updatePolicy>daily</updatePolicy>
        <checksumPolicy>fail</checksumPolicy>
    </releases>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
        <checksumPolicy>ignore</checksumPolicy>
    </snapshots>
</repository>

<dependency>
    <groupId>com.fortysevendeg.android</groupId>
    <artifactId>swipelistview</artifactId>
    <version>1.0-SNAPSHOT</version>
    <type>apklib</type>
</dependency>
```
## APKLib and others

You can get the releases, snapshots and other forms in which SwipeListView is distributed from the
[Downloads](https://github.com/47deg/android-swipelistview/downloads) page.

## Dependencies

SwipeListView depends on the following libraries.

- com.nineoldandroids 

SwipeListView expect that you include one the google android [compatibility libraries](http://developer.android.com/intl/es/tools/extras/support-library.html) in order to use Loaders in versions that do not support them natively.
Depends on your requirements you may choose to include one of the following...

- com.google.android :
    - support-v4 (Available in Maven Central)

# XML Usage

If you decide to use SwipeListView as a view, you can define it in your xml layouts like this:

```xml
    <com.fortysevendeg.android.swipelistview.SwipeListView
            xmlns:swipe="http://schemas.android.com/apk/res-auto"
            android:id="@+id/example_lv_list"
            android:listSelector="#00000000"
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            swipe:swipeFrontView="@+id/front"
            swipe:swipeBackView="@+id/back"
            swipe:swipeActionLeft="reveal | dismiss"
            swipe:swipeActionRight="reveal | dismiss"
            swipe:swipeMode="none | both | right | left"
            swipe:swipeCloseAllItemsWhenMoveList="true"
            swipe:swipeOpenOnLongPress="true"
            swipe:swipeAnimationTime="2000"
            swipe:swipeOffsetLeft="50dp"
            swipe:swipeOffsetRight="50dp"
            />
```

* `swipeFrontView` - TODO
* `swipeBackView` - TODO
* `swipeActionLeft` - TODO
* `swipeActionRight` - TODO
* `swipeMode` - TODO
* `swipeCloseAllItemsWhenMoveList` - TODO
* `swipeOpenOnLongPress` - TODO
* `swipeAnimationTime` - TODO
* `swipeOffsetLeft` - TODO
* `swipeOffsetRight` - TODO
