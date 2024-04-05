import React from 'react';


class Modal extends React.Component {

  constructor(props){
    super(props);
      this.setWrapperRef = this.setWrapperRef.bind(this);
      this.handleClickOutside = this.handleClickOutside.bind(this);
    }

  componentDidMount() {
     document.addEventListener('mousedown', this.handleClickOutside);
 }

 componentWillUnmount() {
     document.removeEventListener('mousedown', this.handleClickOutside);
 }

 setWrapperRef(node) {
     this.wrapperRef = node;
 }

 /**
  * Alert if clicked on outside of element
  */
 handleClickOutside(event) {
   if (this.wrapperRef && !this.wrapperRef.contains(event.target)) {

     if(!this.props.clickInput){
        this.props.closeModal();
     }
   }
 }

 render() {
    // Render nothing if the "show" prop is false
    if(!this.props.show) {
      return null;
    }

    // The gray background
     const backdropStyle = {
       position: 'fixed',
       top: 0,
       bottom: 0,
       left: 0,
       right: 0,
       backgroundColor: 'rgba(255,255,255,0.6)',
       padding: 50,
       zIndex: 1000
     };

     // The modal "window"
     const modalStyle = {
        width : this.props.width ? this.props.widthSize ? this.props.widthSize : '420px' : '',
        height : this.props.widthTrue ? " " : this.props.width ? this.props.widthSize || (this.props.height && this.props.height.length !== 0) ? this.props.type === 'create-circle' ? '800px' : '90%' : '67%' : '',
        maxHeight: this.props.widthSize && this.props.heightSize ? '640px' : '',
        backgroundColor: '#fff',
        position: 'absolute',
        top: '50%',
        left: '50%',
        right: 'auto',
        bottom: 'auto',
        marginRight: '-50%',
        transform: 'translate(-50%, -50%)',
        boxShadow: this.props.noBoxShadow ? 'none' : ' 0 8px 10px 0 rgba(0, 0, 0, 0.24)',
        overflow: this.props.noBoxShadow ? 'hidden' : 'scroll',
        zIndex: 20000000
       };

       //
      return (
        <div className="backdrop" style={backdropStyle}>
          <div className="modal" style={modalStyle} ref={this.setWrapperRef}>
            {this.props.children}
          </div>
        </div>
      );
  }
}

export default Modal;
