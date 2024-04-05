import React, {Component} from 'react';
import Slider from 'react-slick';
import _ from 'lodash';
import Modal from '../../CircleHome/components/Modal';
import moment from 'moment';


class PhotoSlider extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showSliderImg:props.showSliderImg
    }

  }

  getMediatime(time){
    return moment.unix(time/1000).format("h:m A");
  }

  getMediaDate(time){
    return moment.unix(time/1000).format("MMMM D, YYYY");
  }

  orderMediaByClicked(media){
    let newMedia = [],
        index,
        element,
        that=this;
    if(media.length > 0){
      media.map((img,index) => {
        if(!_.isEmpty(that.props.content) && img.uuid === that.props.content.uuid){
          element = media[index];
          newMedia = media;
          newMedia.splice(index, 1);
          newMedia.splice(0, 0, element);
        }
      })
    }

    return newMedia;
  }

  renderAllPhotos(){
    let showSliderPhotos;

    showSliderPhotos=this.orderMediaByClicked(this.props.media).map((item,index) => {
        return(
          <div key={index} className="test-main-main-main">
            <div className="meida-img-container">
              <img alt="content" src={item.content.content} className="chat-image-clicked"/>
            </div>
            <div className="media-container">
              <img src={item.author.photo} className="media-profile" alt={item.author.name}/>
              <div className="cursor-poiner" onClick={() => this.props.showUserModalDetails(item.author)}>
                <p className="posted">Posted by</p>
                <p className="media-name"><strong>{item.author.name}</strong> at {this.getMediatime(item.creationTime)} on {this.getMediaDate(item.creationTime)}</p>
              </div>
            </div>
          </div>
        )
      });

    return showSliderPhotos;
  }


  render() {

    let settings = {
     dots: false,
     arrows: this.props.media && this.props.media.length > 1 ? true  : false,
     infinite: true,
     speed: 500,
     slidesToShow: 1,
     slidesToScroll: 1
   };

    return(
      <Modal show={this.state.showSliderImg}
             closeModal={() => this.props.onClosePhotoSlider()}
             width={true}
             widthTrue={true}>
        <button className="x-btn photo-modal"
          onClick={() => this.props.onClosePhotoSlider()}>
          <img src={require('../../../assets/images/close-shape-trans@2x.png')} alt="close"/>
        </button>
        <Slider {...settings}>
          {this.renderAllPhotos()}
        </Slider>
      </Modal>
    )
  }

}

export default PhotoSlider;
