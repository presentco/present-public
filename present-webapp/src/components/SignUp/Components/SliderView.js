import React, { Component} from 'react';
import './style.css';
import _ from 'lodash';
import Slider from 'react-slick';

function SampleNextArrow(props) {
  const {className, onClick} = props
  return (
    <div
      className={className}
      onClick={onClick}>
      <img src={require('../../../assets/images/right-arrow.svg')} alt="right"/>
    </div>
  );
}

function SamplePrevArrow(props) {
  const {className, onClick} = props
  return (
    <div
      className={className}
      onClick={onClick}>
      <img src={require('../../../assets/images/left-arrow.svg')} alt="left"/>
    </div>
  );
}


class SliderView extends Component {
  constructor(props){
    super(props);
    this.state={
      showDots:false,
    }
  }


    getTitle(){
      let type = this.props.type,
          title = ""
      if(type === 'category'){
        title = "Explore this category";
      }

      if(type === "user"){
        title = "Join Present with...";
      }

      if(type === "circle"){
        title = "Join this circle";
      }

      return title;
    }

    getStaticMap(circle){
      return `https://maps.googleapis.com/maps/api/staticmap?size=1600x360&zoom=18&scale=2&maptype=roadmap&markers=color:red%7C${circle.location.latitude},${circle.location.longitude}&key=AIzaSyDAkC7ZPpRvdt2Nh1NS7fKxKJis6ZTf6N4`;
    }

  renderCardBasedOnInfo(){
    let group =  this.props.urlInfo,
        coverPhoto = "",
        cardInfo = <span/>;

        if(!_.isEmpty(group) && group !== undefined){


          if(this.props.type === 'circle'){
            coverPhoto = group.cover ? group.cover.content + '=w587-h330-n-rj' : this.getStaticMap(group);
            cardInfo = (
              <div className="cursor-poiner" onClick={() => this.props.onClickCard()}>
                <div className="befor-log-co">
                  <div className="card-main-bg">
                    <div className="aspect-ratio">
                      <img alt="" src={coverPhoto} className="cover-photo"/>
                    </div>
                  </div>

                  <div className="info-container cir befor-log-cir">
                    <div className="an-container">
                      <div className="location-cont for-signup-card">
                        <span className="circle-title">
                          {group.title}
                        </span>
                      </div>

                      <div className="location-cont-locationname">
                        <img alt="pin" src={require('../../../assets/images/page-1.svg')} className="loc-icon-cir before-login-img"/>
                        <span className="circle-location">{group.locationName}</span>
                      </div>
                    </div>
                    </div>
                  </div>
                </div>
            );
          } else if(this.props.type === 'user'){
            cardInfo = (
              <div className="user-login-container cursor-poiner" onClick={() => this.props.onClickCard()}>
                <img src={group.photo} className="user-login-photo" />
                <p className="user-login-name">{group.name}</p>
              </div>
            )
          } else if(this.props.type === 'category'){
            cardInfo=(
              <div className="befor-log-co cursor-poiner" onClick={() => this.props.onClickCard()}>
                <div className="aspect-ratio">
                  <img alt="" src={`https://present.co/images/${group.name}-category@2x.png`} className="cover-photo border-radius-5 box-shadow"/>
                </div>
                <p className="category-title">{group.name}</p>
              </div>
            )
          }
        }

    return cardInfo;
  }


  renderUrlInfo(type){

    let explore = "connect.svg";
    if(type === "circle"){
      explore = 'Inspire.svg';
    } else if(type === "category"){
      explore = "explore@2x.png";
    }

    return (
      <div>
        <div className="show-for-small-down">
          <div className="small-12 small-centered columns">
            <p className="slider-title-small">{this.getTitle()}</p>
            {this.renderCardBasedOnInfo()}
          </div>
        </div>
        <div className="show-for-medium-up">
          <div className="medium-6 large-6 columns">
            <div className="position-relative left-22">
              {type === 'category' ? <img alt="logo"
                src={require(`../../../assets/images/${explore}`)}
                className={type === 'vibe' ? "slider-vibe" : "slider-photo"}  /> : <span/>}

            </div>
          </div>
          <div className="small-12 medium-6 large-6 padding-left-8 padding-top columns">
            <p className="slider-title">{this.getTitle()}</p>
            {this.renderCardBasedOnInfo()}
          </div>
        </div>
      </div>
    )
  }

  renderSliderPages(data, title, photo){
    return(
      <div>
        <div className="show-for-small-down">
          <div className="small-12 small-centered slide-small-container columns">
            <img alt="logo"
              src={require(`../../../assets/images/${photo}@2x.png`)}
              className="slider-photo-small" />
          </div>
          <div className="small-12 small-centered columns">
            <p className="slider-title-small">{title}</p>
            <p className="slogan-small">{data}</p>
          </div>

        </div>
        <div className="show-for-medium-up">
          <div className="medium-6 large-6 columns">
            <div className="position-relative left-12">
                <img alt="logo"
                  src={require(`../../../assets/images/${photo}@2x.png`)}
                  className="slider-photo"/>
            </div>

          </div>
          <div className="small-12 medium-6 large-6 padding-left-8 padding-top columns">
            <p className="slider-title">{title}</p>
            <p className="slogan">{data}</p>
          </div>
        </div>
      </div>

    )
  }
  render() {
    let settings = {
      dots: this.state.showDots,
      infinite: true,
      speed: 500,
      slidesToShow: 1,
      slidesToScroll: 1,
      nextArrow: <SampleNextArrow />,
      prevArrow: <SamplePrevArrow />
    };

    return(
    <div>
      {this.props.type !== 'app' ?
      this.renderUrlInfo(this.props.type) :
      <Slider {...settings}>
          {/*this.renderSliderPages("You decide who to chat, meet, and share experiences with in your community", 'Find Your Good Vibe Tribe', 'vibe')*/}
          {this.renderSliderPages("what people are doing nearby; parties, sports, concerts, and more!", 'Discover', 'discover')}
          {this.renderSliderPages("interesting group activites; meet new people who share your interests", 'Join', 'explore')}
          {this.renderSliderPages("chat with people and add them as friends", 'Stay connected', 'chat')}
      </Slider>
    }
    </div>
    )
  }
}

export default SliderView;
