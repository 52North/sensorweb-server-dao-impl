#!/bin/bash -e

# taken from:
# https://tecadmin.net/install-ruby-on-rails-on-ubuntu/
sudo apt-get -qq update && sudo apt-get -qq install \
    curl \
    gnupg2

cd
gpg2 --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3
# get and install ruby
sudo curl -sSL https://get.rvm.io | bash -s stable

# setup environment in current shell
sudo . /etc/profile.d/rvm.sh
sudo rvm requirements
sudo rvm list known
sudo rvm install 2.2.4
sudo rvm use 2.2.4 --default
ruby --version 

# taken from:
# https://gorails.com/setup/ubuntu/14.04

#RUBY_VERSION=2.4.0

#sudo apt-get -qq update && sudo apt-get -qq install \
#    zlib1g-dev \
#    build-essential \
#    libssl-dev \
#    libreadline-dev \
#    libyaml-dev \
#    libxml2-dev \
#    libxslt1-dev \
#    python-software-properties \
#    libffi-dev

#cd
#git clone https://github.com/rbenv/rbenv.git ~/.rbenv
#echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
#echo 'eval "$(rbenv init -)"' >> ~/.bashrc
#exec $SHELL

#git clone https://github.com/rbenv/ruby-build.git ~/.rbenv/plugins/ruby-build
#echo 'export PATH="$HOME/.rbenv/plugins/ruby-build/bin:$PATH"' >> ~/.bashrc
#exec $SHELL

#rbenv install "$RUBY_VERSION"
#rbenv global "$RUBY_VERSION"
#ruby --version

#gem install bundler

#rbenv rehash