#!/usr/bin/env bash

echo "Re-building with target Java 7 (such that the compiled .class files will be compatible with as many JVMs as possible)..."

cd src

# build build build!
javac -encoding utf8 -d ../bin -bootclasspath ../other/java7_rt.jar -source 1.7 -target 1.7 @sourcefiles.list

cd ..



echo "Creating the release file assBrowser.zip..."

mkdir release

cd release

mkdir assBrowser

# copy the main files
cp -R ../bin assBrowser
cp ../UNLICENSE assBrowser
cp ../README.md assBrowser
cp ../run.sh assBrowser
cp ../run.bat assBrowser

# convert \n to \r\n for the Windows files!
cd assBrowser
awk 1 ORS='\r\n' run.bat > rn
mv rn run.bat
cd ..

# create a version tag right in the zip file
cd assBrowser
version=$(./run.sh --version_for_zip)
echo "$version" > "$version"
cd ..

# zip it all up
zip -rq assBrowser.zip assBrowser

mv assBrowser.zip ..

cd ..
rm -rf release

echo "The file assBrowser.zip has been created in $(pwd)"
